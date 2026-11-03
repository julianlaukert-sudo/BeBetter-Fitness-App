package de.bebetter.backend.service;

import de.bebetter.backend.domain.Role;
import de.bebetter.backend.domain.User;
import de.bebetter.backend.dto.AdminUserDto;
import de.bebetter.backend.dto.StatsResponse;
import de.bebetter.backend.dto.UserDeletedResponse;
import de.bebetter.backend.repository.ExerciseMarkRepository;
import de.bebetter.backend.repository.ProfileRepository;
import de.bebetter.backend.repository.TrainingPlanRepository;
import de.bebetter.backend.repository.UserRepository;
import de.bebetter.backend.web.ApiException;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// KI-Log Nr. 18
@Service
public class AdminService {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private final UserRepository users;
    private final ProfileRepository profiles;
    private final TrainingPlanRepository plans;
    private final ExerciseMarkRepository marks;
    private final TimeProvider time;

    public AdminService(UserRepository users, ProfileRepository profiles,
                        TrainingPlanRepository plans, ExerciseMarkRepository marks,
                        TimeProvider time) {
        this.users = users;
        this.profiles = profiles;
        this.plans = plans;
        this.marks = marks;
        this.time = time;
    }

    private AdminUserDto dto(User u) {
        return new AdminUserDto(u.getId(), u.getName(), u.getEmail(),
                u.getRegisteredAt().atZoneSameInstant(BERLIN).toLocalDate(), u.getRole().name());
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> userList() {
        return users.findAllByOrderByRegisteredAtDesc().stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public AdminUserDto user(Long id) {
        return users.findById(id).map(this::dto)
                .orElseThrow(() -> new ApiException("User not found."));
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        var today = time.today();
        return new StatsResponse(users.countByRole(Role.USER), plans.count(),
                marks.countByDateBetweenAndAutomaticFalse(today.minusDays(6), today));
    }

    @Transactional
    public UserDeletedResponse delete(Long id) {
        profiles.findForPlan(id);   // gleiche Profilsperre wie Trainingsdienste (falls Profil existiert)
        User u = users.findForDelete(id)
                .orElseThrow(() -> new ApiException("User not found."));
        if (u.isAdmin()) throw new ApiException("Admin cannot be deleted.");
        users.delete(u);            // Datenbank-Kaskade entfernt Profil, Pläne, Einheiten, Übungen, Markierungen
        users.flush();
        return new UserDeletedResponse("User deleted.", id);
    }
}