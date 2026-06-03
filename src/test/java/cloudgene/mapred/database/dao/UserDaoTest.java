package cloudgene.mapred.database.dao;

import cloudgene.mapred.test.TestApplication;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.util.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserDaoTest {

	private UserDao dao;

	@BeforeEach
	public void setup() throws Exception {
		TestApplication application = new TestApplication();
		Database db = application.getDatabase();
		dao = new UserDao(db);
	}

	@Test
	public void testFindAll() {
		// Existing users are returned in USERNAME ALPHABETICAL order.
		// - 'admin' is injected by Fixtures (from the Application constructor).
		// - 'admin', 'user', and 'public' are injected by TestApplication (callback on the Application constructor).
		// - 'public' can additionally be injected by PublicUser (called on UserDao.delete()).

		// findAll() with no parameters returns all users.
		List<User> defaultUsers = dao.findAll();
		assertNotNull(defaultUsers);
		assertEquals(3, defaultUsers.size());
		assertEquals(
				List.of("admin", "public", "user"),
				extractUsernames(defaultUsers));

		// findAll(offset, limit) returns 'limit' entries, starting at 'offset'.
		List<User> top2 = dao.findAll(0, 2);
		assertNotNull(top2);
		assertEquals(
				List.of("admin", "public"),
				extractUsernames(top2));

		List<User> third = dao.findAll(2, 1);
		assertNotNull(third);
		assertEquals(
				List.of("user"),
				extractUsernames(third));

		// SQL errors lead to empty (non-null) output. Here, offset is negative:
		List<User> wrongOffset = dao.findAll(-1, 2);
		assertNotNull(wrongOffset);
		assertTrue(wrongOffset.isEmpty());

		// ...and here, limit is <= 0
		List<User> wrongLimit = dao.findAll(0, -1);
		assertNotNull(wrongLimit);
		assertTrue(wrongLimit.isEmpty());
	}

	@Test
	public void testFindById() {
		User observed;

		// User IDs are sequential and 1-indexed. 'admin' is the first injected user.
		observed = dao.findById(1);
		assertNotNull(observed);
		assertEquals("admin", observed.getUsername());

		// ...and 'public' is the third injected user.
		observed = dao.findById(3);
		assertNotNull(observed);
		assertEquals("public", observed.getUsername());

		// On error, null is returned.
		observed = dao.findById(-1);
		assertNull(observed);
	}

	@Test
	public void testFindByUsername() {
		// With an existing username, we get back the correct user:
		for (String username : List.of("admin", "public", "user")) {
			User observed = dao.findByUsername(username);
			assertNotNull(observed);
			assertEquals(username, observed.getUsername());
		}

		// Otherwise, we get null:
		for (String username : List.of("", " ", "not_a_user", "fakefake")) {
			User observed = dao.findByUsername(username);
			assertNull(observed);
		}
	}

	@Test
	public void testInsert() {
		List<User> before = dao.findAll();
		assertNotNull(before);

		String username = "test-user";
		String pwdHash = HashUtil.hashPassword("I 4m a STR$NG pwd!?!!111");

		User user = new User();
		user.setUsername(username);
		user.setPassword(pwdHash);
		user.setRoles(new String[] { User.ROLE_USER });

		dao.insert(user);

		List<User> after = dao.findAll();
		assertNotNull(after);
		assertEquals(before.size() + 1, after.size());

		Optional<User> maybeFound = after.stream()
				.filter(u -> Objects.equals(u.getUsername(), "test-user"))
				.findFirst();

		assertTrue(maybeFound.isPresent());

		User found = maybeFound.get();
		assertEquals(username, found.getUsername());
		assertEquals(pwdHash, found.getPassword());
		assertEquals(after.size(), found.getId());
		assertTrue(found.hasRole(User.ROLE_USER));
	}

	private List<String> extractUsernames(List<User> users) {
		return users.stream().map(User::getUsername).toList();
	}
}
