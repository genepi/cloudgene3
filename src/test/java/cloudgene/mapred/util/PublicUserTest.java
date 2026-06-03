package cloudgene.mapred.util;

import cloudgene.mapred.test.TestApplication;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.UserDao;
import cloudgene.mapred.database.util.Database;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class PublicUserTest {

    @Inject
    TestApplication application;

    @Test
    public void getUser() {
        Database db = application.getDatabase();
        UserDao userDao = new UserDao(db);

        // Default users have been injected.
        List<User> usersAfterInit = userDao.findAll();
        assertFalse(usersAfterInit.isEmpty());

        // Default users include public
        Optional<User> publicBefore = usersAfterInit.stream()
                .filter(user -> user.getUsername().equals("public"))
                .findFirst();
        assertTrue(publicBefore.isPresent());

        // Method under test
        User publicUser = PublicUser.getUser(db);

        // PublicUser.getUser() fetches the 'public' user from the DB, and it matches the one we fished before.
        assertNotNull(publicUser);
        assertEquals(publicUser, publicBefore.get());

        // Nothing changed in the DB so far.
        List<User> usersAfterPublic = userDao.findAll();
        assertEquals(usersAfterInit, usersAfterPublic);

        // TODO(Marc): This should not be possible.
        // Let's delete 'public'.
        userDao.delete(publicUser);

        // Check it's gone
        List<User> usersAfterDelete = userDao.findAll();
        assertEquals(usersAfterPublic.size() - 1, usersAfterDelete.size());

        boolean publicPresent = usersAfterDelete.stream()
                .anyMatch(user -> user.getUsername().equals("public"));
        assertFalse(publicPresent);

        // Testing again re-adds the user.
        User newPublicUser = PublicUser.getUser(db);

        // Same data re-added
        assertEquals(publicUser, newPublicUser);

        // Check it's back
        List<User> usersAfterAdd = userDao.findAll();
        assertEquals(usersAfterInit.size(), usersAfterAdd.size());

        publicPresent = usersAfterAdd.stream()
                .anyMatch(user -> user.getUsername().equals("public"));
        assertTrue(publicPresent);
    }
}
