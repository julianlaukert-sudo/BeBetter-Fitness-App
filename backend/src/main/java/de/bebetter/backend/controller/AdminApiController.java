package de.bebetter.backend.controller;

import de.bebetter.backend.dto.AdminUserDto;
import de.bebetter.backend.dto.StatsResponse;
import de.bebetter.backend.dto.UserDeletedResponse;
import de.bebetter.backend.service.AdminService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** JSON-API für Admins – verlangt ein ADMIN-Bearer-Token (/admin/** in SecurityConfig). */
@RestController
public class AdminApiController {

    private final AdminService admin;

    public AdminApiController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/admin/users")
    public List<AdminUserDto> users() { return admin.userList(); }

    @GetMapping("/admin/stats")
    public StatsResponse stats() { return admin.stats(); }

    @DeleteMapping("/admin/users/{id}")
    public UserDeletedResponse delete(@PathVariable Long id) { return admin.delete(id); }
}