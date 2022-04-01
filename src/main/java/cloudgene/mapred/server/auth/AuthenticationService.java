package cloudgene.mapred.server.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.RandomStringUtils;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import cloudgene.mapred.core.ApiToken;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.UserDao;
import cloudgene.mapred.server.Application;
import cloudgene.mapred.server.responses.ValidatedApiTokenResponse;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.authentication.AuthorizationException;
import io.micronaut.security.token.jwt.generator.JwtTokenGenerator;
import io.micronaut.security.token.jwt.validator.JwtTokenValidator;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

@Singleton
public class AuthenticationService {

	private static final String MESSAGE_VALID_API_TOKEN = "API Token was created by %s and is valid.";

	private static final String MESSAGE_INVALID_API_TOKEN = "Invalid API Token.";

	@Inject
	protected Application application;

	@Inject
	protected JwtTokenGenerator generator;

	@Inject
	protected JwtTokenValidator validator;

	public static String ATTRIBUTE_TOKEN_TYPE = "token_type";

	public static String ATTRIBUTE_API_HASH = "api_hash";

	public User getUserByAuthentication(Authentication authentication) {
		return getUserByAuthentication(authentication, AuthenticationType.ACCESS_TOKEN);
	}

	public User getUserByAuthentication(Authentication authentication, AuthenticationType authenticationType) {

		User user = null;
		if (authentication != null) {
			UserDao userDao = new UserDao(application.getDatabase());
			user = userDao.findByUsername(authentication.getName());
			Map<String, Object> attributes = authentication.getAttributes();

			if (attributes.containsKey(ATTRIBUTE_TOKEN_TYPE)) {

				String tokenType = attributes.get(ATTRIBUTE_TOKEN_TYPE).toString();

				if (tokenType.equalsIgnoreCase(AuthenticationType.API_TOKEN.toString())) {

					if (authenticationType == AuthenticationType.API_TOKEN
							|| authenticationType == AuthenticationType.ALL_TOKENS) {
						if (user.getApiToken().equals(attributes.get(ATTRIBUTE_API_HASH))) {
							return user;
						}
					}

				} else if (tokenType.equalsIgnoreCase(AuthenticationType.ACCESS_TOKEN.toString())) {

					if (authenticationType == AuthenticationType.ACCESS_TOKEN
							|| authenticationType == AuthenticationType.ALL_TOKENS) {
						return user;
					}

				}

			} else {

				if (authenticationType == AuthenticationType.ACCESS_TOKEN
						|| authenticationType == AuthenticationType.ALL_TOKENS) {
					return user;
				}

			}

			throw new AuthorizationException(authentication);

		}

		throw new AuthenticationException();

	}

	public ApiToken createApiToken(User user, int lifetime) {

		String hash = RandomStringUtils.random(30);

		Map<String, Object> attribtues = new HashMap<String, Object>();
		attribtues.put(ATTRIBUTE_TOKEN_TYPE, AuthenticationType.API_TOKEN.toString());
		attribtues.put(ATTRIBUTE_API_HASH, hash);
		// addition attributes that are needed by imputationbot
		attribtues.put("username", user.getUsername());
		attribtues.put("name", user.getFullName());
		attribtues.put("mail", user.getMail());
		attribtues.put("api", true);

		Authentication authentication2 = Authentication.build(user.getUsername(), attribtues);
		Optional<String> token = generator.generateToken(authentication2, lifetime);

		return new ApiToken(token.get(), hash);

	}

	public ValidatedApiTokenResponse validateApiToken(String token) {

		Publisher<Authentication> authentication = validator.validateToken(token, null);

		return Mono.<ValidatedApiTokenResponse>create(emitter -> {
			authentication.subscribe(new Subscriber<Authentication>() {

				private Subscription subscription;

				@Override
				public void onComplete() {

				}

				@Override
				public void onError(Throwable throwable) {
					emitter.error(throwable);
				}

				@Override
				public void onNext(Authentication authentication) {
					try {
						User user = getUserByAuthentication(authentication, AuthenticationType.API_TOKEN);
						if (user == null) {
							emitter.success(ValidatedApiTokenResponse.error(MESSAGE_INVALID_API_TOKEN));
						} else {
							emitter.success(ValidatedApiTokenResponse.valid(MESSAGE_VALID_API_TOKEN));
						}
					} catch (Exception e) {
						emitter.success(ValidatedApiTokenResponse.error(MESSAGE_INVALID_API_TOKEN));
					}
					subscription.request(1);
				}

				@Override
				public void onSubscribe(Subscription subscription) {
					this.subscription = subscription;
					subscription.request(1);
				}

			});
		}).block();

	}

}