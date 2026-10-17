package de.bebetter.backend.service;

import de.bebetter.backend.domain.Profile;
import de.bebetter.backend.domain.User;
import de.bebetter.backend.dto.ProfileRequest;
import de.bebetter.backend.dto.ProfileResponse;
import de.bebetter.backend.repository.ProfileRepository;
import de.bebetter.backend.repository.UserRepository;
import de.bebetter.backend.validation.ProfileRules;
import de.bebetter.backend.web.ApiException;
import de.bebetter.backend.web.InvalidInputException;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final ProfileRepository profiles;
    private final UserRepository users;
    private final Clock clock;

    public ProfileService(ProfileRepository profiles, UserRepository users, Clock clock) {
        this.profiles = profiles;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProfileResponse load(Long userId) {
        return toResponse(profiles.findById(userId)
                .orElseThrow(() -> new ApiException("Profile not found.")));
    }

    @Transactional
    public ProfileResponse create(Long userId, ProfileRequest request) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException("Token is wrong."));
        if (user.isAdmin()) throw new ApiException("Access denied.");

        var personal = request == null ? null : request.personal();
        var training = request == null ? null : request.training();

        Map<String, String> errors = ProfileRules.check(
                personal == null ? null : personal.age(),
                personal == null ? null : personal.gender(),
                personal == null ? null : personal.height(),
                personal == null ? null : personal.weight(),
                training == null ? null : training.goal(),
                training == null ? null : training.fitnessLevel(),
                training == null ? null : training.trainingDays());
        if (!errors.isEmpty()) throw new InvalidInputException(errors);

        // Ein zweites POST überschreibt kein bestehendes Profil.
        if (profiles.existsById(userId)) throw new ApiException("Profile already exists.");

        Profile profile = new Profile(userId, OffsetDateTime.now(clock));
        profile.setAge(personal.age());
        profile.setGender(personal.gender());
        profile.setHeightCm(personal.height());
        profile.setWeightKg(personal.weight().setScale(1, RoundingMode.HALF_UP));
        profile.setGoal(training.goal());
        profile.setFitnessLevel(training.fitnessLevel());
        profile.setTrainingDayList(training.trainingDays());
        return toResponse(profiles.saveAndFlush(profile));
    }

    private ProfileResponse toResponse(Profile p) {
        return new ProfileResponse(p.getUserId(),
                new ProfileRequest.Personal(p.getAge(), p.getGender(),
                        p.getHeightCm(), p.getWeightKg()),
                new ProfileRequest.Training(p.getGoal(), p.getFitnessLevel(),
                        p.getTrainingDayList()),
                toDate(p.getCreatedAt()),
                toDate(p.getUpdatedAt()),
                false); // planExists: Trainingsplan folgt in Sprint 3
    }

    /** Datum in Europe/Berlin (Zone der Clock-Bean). */
    private LocalDate toDate(OffsetDateTime time) {
        return time.atZoneSameInstant(clock.getZone()).toLocalDate();
    }
}