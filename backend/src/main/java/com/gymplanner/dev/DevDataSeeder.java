package com.gymplanner.dev;

import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymMembership;
import com.gymplanner.gym.GymMembershipRepository;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.user.User;
import com.gymplanner.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dane testowe dla środowiska dev (profil {@code dev}). Uruchamia się tylko na pustej bazie
 * (gdy nie istnieje użytkownik {@value #ANNA}).
 */
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    static final String ANNA = "anna@example.com";
    static final String JAN = "jan@example.com";
    static final String PASSWORD = "Password123";

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository users;
    private final GymRepository gyms;
    private final GymMembershipRepository memberships;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(UserRepository users, GymRepository gyms, GymMembershipRepository memberships,
            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.gyms = gyms;
        this.memberships = memberships;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByEmail(ANNA)) {
            return;
        }
        User anna = users.save(new User(ANNA, passwordEncoder.encode(PASSWORD), "Anna"));
        User jan = users.save(new User(JAN, passwordEncoder.encode(PASSWORD), "Jan"));

        Gym arena = gyms.save(new Gym("Fitness Arena Centrum", "Warszawa", "ul. Marszałkowska 100",
                "Duża siłownia w centrum, strefa wolnych ciężarów i cardio.", anna));
        Gym iron = gyms.save(new Gym("Iron Gym Kazimierz", "Kraków", "ul. Starowiślna 20",
                "Klimatyczna siłownia trójbojowa.", jan));

        memberships.save(new GymMembership(anna, arena));
        memberships.save(new GymMembership(jan, arena));
        memberships.save(new GymMembership(jan, iron));

        log.info("Dev data seeded: users {} / {} (password: {})", ANNA, JAN, PASSWORD);
    }
}
