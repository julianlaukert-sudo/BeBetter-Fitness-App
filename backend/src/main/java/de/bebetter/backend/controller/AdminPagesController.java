package de.bebetter.backend.controller;

import de.bebetter.backend.domain.User;
import de.bebetter.backend.repository.UserRepository;
import de.bebetter.backend.security.TokenBlocklist;
import de.bebetter.backend.service.AdminService;
import de.bebetter.backend.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
// KI-Log Nr. 18
/** Serverseitig gerenderter Adminbereich (MPA). */
@Controller
public class AdminPagesController {

    private final AdminService admin;
    private final UserRepository users;
    private final JwtDecoder decoder;
    private final TokenBlocklist blocklist;
    private final String frontend;
    private final HttpSessionSecurityContextRepository sessions =
            new HttpSessionSecurityContextRepository();

    public AdminPagesController(AdminService admin, UserRepository users, JwtDecoder decoder,
                                TokenBlocklist blocklist,
                                @Value("${FRONTEND_URL:http://localhost:5173}") String frontend) {
        this.admin = admin;
        this.users = users;
        this.decoder = decoder;
        this.blocklist = blocklist;
        this.frontend = frontend.replaceAll("/+$", "");
    }

    /** Übergang aus dem React-Login: Token kommt einmal im POST-Body, nie in der URL. */
    @PostMapping("/admin/sso")
    public String sso(@RequestParam String token, HttpServletRequest req,
                      HttpServletResponse res) throws IOException {
        if (!frontend.equals(req.getHeader("Origin"))) {   // fester Origin
            res.sendError(403);
            return null;
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(token);
        } catch (JwtException ex) {
            return "redirect:" + frontend + "/login?adminError=1";
        }
        User user = users.findById(Long.valueOf(jwt.getSubject())).orElse(null);
        if (user == null || !user.isAdmin() || !"ADMIN".equals(jwt.getClaimAsString("role"))) {
            res.sendError(403);
            return null;
        }
        // Alte Session verwerfen, neue anlegen und SecurityContext ausdrücklich speichern
        var old = req.getSession(false);
        if (old != null) old.invalidate();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        SecurityContextHolder.setContext(context);
        sessions.saveContext(context, req, res);
        req.getSession().setAttribute("adminName", user.getName());
        blocklist.block(jwt.getId(), jwt.getExpiresAt());   // Übergabe-Token verbrauchen (Aufruf wie in AuthService)
        return "redirect:/admin/overview";
    }

    @ModelAttribute("adminName")
    public String adminName(HttpServletRequest req) {
        var s = req.getSession(false);
        return s == null ? "Admin" : String.valueOf(s.getAttribute("adminName"));
    }

    @GetMapping({"/admin", "/admin/"})
    public String start() {
        return "redirect:/admin/overview";
    }

    @GetMapping("/admin/overview")
    public String overview(Model model) {
        model.addAttribute("stats", admin.stats());
        model.addAttribute("latest", admin.userList().stream().limit(5).toList());
        return "admin/overview";
    }

    @GetMapping("/admin/accounts")
    public String accounts(Model model) {
        model.addAttribute("users", admin.userList());
        return "admin/accounts";
    }

    /** GET zeigt nur die Sicherheitsabfrage – löscht nichts. */
    @GetMapping("/admin/accounts/{id}/delete")
    public String confirmDelete(@PathVariable Long id, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("user", admin.user(id));
            return "admin/account-delete";
        } catch (ApiException ex) {
            flash.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/accounts";
        }
    }

    /** Erst das bestätigte POST-Formular (mit CSRF) löscht dauerhaft. */
    @PostMapping("/admin/accounts/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            admin.delete(id);
            flash.addFlashAttribute("success", "User and related data deleted.");
        } catch (ApiException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/accounts";
    }
}