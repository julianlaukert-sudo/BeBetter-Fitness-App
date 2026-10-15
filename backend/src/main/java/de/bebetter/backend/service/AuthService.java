package de.bebetter.backend.service;

import de.bebetter.backend.domain.Role;
import de.bebetter.backend.domain.User;
import de.bebetter.backend.dto.LoginRequest;
import de.bebetter.backend.dto.LoginResponse;
import de.bebetter.backend.dto.MessageResponse;
import de.bebetter.backend.dto.RegistrationRequest;
import de.bebetter.backend.dto.RegistrationResponse;
import de.bebetter.backend.repository.ProfileRepository;
import de.bebetter.backend.repository.UserRepository;
import de.bebetter.backend.security.TokenBlocklist;
import de.bebetter.backend.security.TokenService;
import de.bebetter.backend.validation.PasswordPolicy;
import de.bebetter.backend.validation.RegistrationRules;
import de.bebetter.backend.web.ApiException;
import de.bebetter.backend.web.InvalidInputException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final TokenBlocklist tokenBlocklist;
    private final Clock clock;
    // Wird bei unbekannter E-Mail geprüft, damit die Antwortzeit nichts verrät
    private final String dummyHash;

    public AuthService(UserRepository userRepository, ProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder, TokenService tokenService,
                       TokenBlocklist tokenBlocklist, Clock clock) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.tokenBlocklist = tokenBlocklist;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("bebetter-dummy-password");
    }

    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        if (request == null) {
            throw new InvalidInputException(Map.of());
        }
        Map<String, String> errors = RegistrationRules.check(request.name(), request.email(), request.password());
        if (!errors.isEmpty()) {
            throw new InvalidInputException(errors);
        }
        String email = RegistrationRules.normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException("User already exists.");
        }
        // Registrierung erzeugt immer USER, ein mitgeschicktes "role"-Feld wird ignoriert
        User user = userRepository.save(new User(request.name().trim(), email,
                passwordEncoder.encode(request.password()), Role.USER, OffsetDateTime.now(clock)));
        return new RegistrationResponse("User created successfully.", user.getId(), user.getName(),
                user.getEmail(), user.getRole().name());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request == null ? "" : RegistrationRules.normalizeEmail(request.email());
        String password = request == null || request.password() == null ? "" : request.password();

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        boolean passwordMatches = !PasswordPolicy.exceedsByteLimit(password)
                && passwordEncoder.matches(password, hash);
        if (user == null || !passwordMatches) {
            throw new ApiException("Email or password is wrong.");
        }

        boolean profileCompleted = profileRepository.existsById(user.getId());
        return new LoginResponse(tokenService.create(user), "Bearer", tokenService.validitySeconds(),
                user.getId(), user.getName(), user.getRole().name(), profileCompleted);
    }

    public MessageResponse logout(Jwt jwt) {
        tokenBlocklist.block(jwt.getId(), jwt.getExpiresAt());
        return new MessageResponse("User logged off.");
    }
}