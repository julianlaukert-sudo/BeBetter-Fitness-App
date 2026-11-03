package de.bebetter.backend.controller;

import de.bebetter.backend.dto.AdminUserDto;
import de.bebetter.backend.dto.StatsResponse;
import de.bebetter.backend.dto.UserDeletedResponse;
import de.bebetter.backend.service.AdminService;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
// KI-Log Nr. 18
/** GraphQL-Variante der Admin-Daten – dieselbe Logik wie Adminseiten und Admin-JSON. */
@Controller
public class AdminGraphQlController {

    private final AdminService admin;

    public AdminGraphQlController(AdminService admin) {
        this.admin = admin;
    }

    @QueryMapping
    public List<AdminUserDto> adminUsers() {
        return admin.userList();
    }

    @QueryMapping
    public StatsResponse adminStats() {
        return admin.stats();
    }

    @MutationMapping
    public UserDeletedResponse deleteUser(@Argument Long id) {
        return admin.delete(id);
    }
}