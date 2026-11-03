package de.bebetter.backend.service;

import de.bebetter.backend.domain.Profile;
import de.bebetter.backend.domain.User;
import de.bebetter.backend.dto.ProfileRequest;
import de.bebetter.backend.dto.ProfileResponse;
import de.bebetter.backend.repository.ProfileRepository;
import de.bebetter.backend.repository.TrainingPlanRepository;
import de.bebetter.backend.repository.UserRepository;
import de.bebetter.backend.validation.ProfileRules;
import de.bebetter.backend.web.ApiException;
import de.bebetter.backend.web.InvalidInputException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
//KI-Log Nr. 11, 13, 17
@Service
public class ProfileService {

    private final ProfileRepository profiles;
    private final UserRepository users;
    private final Clock clock;
    private final TrainingPlanRepository plans;
    private final TimeProvider time;

    public ProfileService(ProfileRepository profiles, UserRepository users, Clock clock,
                          TrainingPlanRepository plans, TimeProvider time) {
        this.profiles = profiles;
        this.users = users;
        this.clock = clock;
        this.plans = plans;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public ProfileResponse load(Long userId) {
        return toResponse(profiles.findById(userId)
                .orElseThrow(() -> new ApiException("Profile not found.")));
    }

    @Transactional
    public ProfileResponse create(Long userId, ProfileRequest request) {
        requireUser(userId);
        validate(request);
        if (profiles.existsById(userId)) throw new ApiException("Profile already exists.");

        var personal = request.personal();
        var training = request.training();
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

    /** Eigenes Profil ändern; relevante Änderungen wirken ab nächstem Montag. */
    @Transactional
    public ProfileResponse update(Long userId, ProfileRequest request) {
        requireUser(userId);
        validate(request);
        Profile profile = profiles.findForPlan(userId)
                .orElseThrow(() -> new ApiException("Profile not found."));

        var personal = request.personal();
        var training = request.training();
        BigDecimal weight = personal.weight().setScale(1, RoundingMode.HALF_UP);

        // Geschlecht zählt NICHT: die Startgewichtsformel hat dafür keinen Faktor
        boolean relevantChange = !Objects.equals(profile.getAge(), personal.age())
                || !Objects.equals(profile.getHeightCm(), personal.height())
                || profile.getWeightKg().compareTo(weight) != 0
                || profile.getGoal() != training.goal()
                || profile.getFitnessLevel() != training.fitnessLevel()
                || !profile.getTrainingDayList().stream().sorted().toList()
                .equals(training.trainingDays().stream().sorted().toList());

        profile.setAge(personal.age());
        profile.setGender(personal.gender());
        profile.setHeightCm(personal.height());
        profile.setWeightKg(weight);
        profile.setGoal(training.goal());
        profile.setFitnessLevel(training.fitnessLevel());
        profile.setTrainingDayList(training.trainingDays());
        profile.setUpdatedAt(OffsetDateTime.now(clock));

        // Nur wenn schon ein Plan existiert; ohne Plan nutzt der erste Plan sofort das neue Profil
        if (relevantChange && plans.existsByUserId(userId)) {
            profile.setReplanFrom(PlanGenerator.mondayOf(time.today()).plusWeeks(1));
        }
        return toResponse(profiles.saveAndFlush(profile));
    }

    private void requireUser(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException("Token is wrong."));
        if (user.isAdmin()) throw new ApiException("Access denied.");
    }

    private void validate(ProfileRequest request) {
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
    }

    private ProfileResponse toResponse(Profile p) {
        return new ProfileResponse(p.getUserId(),
                new ProfileRequest.Personal(p.getAge(), p.getGender(),
                        p.getHeightCm(), p.getWeightKg()),
                new ProfileRequest.Training(p.getGoal(), p.getFitnessLevel(),
                        p.getTrainingDayList()),
                toDate(p.getCreatedAt()),
                toDate(p.getUpdatedAt()),
                plans.existsByUserId(p.getUserId()),
                p.getReplanFrom());
    }

    private LocalDate toDate(OffsetDateTime t) {
        return t.atZoneSameInstant(clock.getZone()).toLocalDate();
    }
}