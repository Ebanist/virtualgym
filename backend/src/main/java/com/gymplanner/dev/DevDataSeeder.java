package com.gymplanner.dev;

import com.gymplanner.equipment.ChangeType;
import com.gymplanner.equipment.Equipment;
import com.gymplanner.equipment.EquipmentCategory;
import com.gymplanner.equipment.EquipmentChange;
import com.gymplanner.equipment.EquipmentChangeRepository;
import com.gymplanner.equipment.EquipmentRepository;
import com.gymplanner.equipment.EquipmentStatus;
import com.gymplanner.equipment.EquipmentTypeRepository;
import com.gymplanner.exercise.ExerciseRepository;
import com.gymplanner.exercise.ExerciseScope;
import com.gymplanner.gym.Gym;
import com.gymplanner.gym.GymMembership;
import com.gymplanner.gym.GymMembershipRepository;
import com.gymplanner.gym.GymRepository;
import com.gymplanner.plan.PlanDay;
import com.gymplanner.plan.PlanItem;
import com.gymplanner.plan.WorkoutPlan;
import com.gymplanner.plan.WorkoutPlanRepository;
import com.gymplanner.user.User;
import java.math.BigDecimal;
import com.gymplanner.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final EquipmentRepository equipment;
    private final EquipmentTypeRepository equipmentTypes;
    private final EquipmentChangeRepository equipmentChanges;
    private final ExerciseRepository exercises;
    private final WorkoutPlanRepository plans;

    public DevDataSeeder(UserRepository users, GymRepository gyms, GymMembershipRepository memberships,
            PasswordEncoder passwordEncoder, EquipmentRepository equipment, EquipmentTypeRepository equipmentTypes,
            EquipmentChangeRepository equipmentChanges, ExerciseRepository exercises, WorkoutPlanRepository plans) {
        this.users = users;
        this.gyms = gyms;
        this.memberships = memberships;
        this.passwordEncoder = passwordEncoder;
        this.equipment = equipment;
        this.equipmentTypes = equipmentTypes;
        this.equipmentChanges = equipmentChanges;
        this.exercises = exercises;
        this.plans = plans;
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

        List<Equipment> arenaEquipment = new ArrayList<>(List.of(
                item(arena, anna, "Wyciąg górny", EquipmentCategory.CABLE, "LAT_PULLDOWN", 2),
                item(arena, anna, "Wyciąg dolny do wiosłowania", EquipmentCategory.CABLE, "SEATED_CABLE_ROW", 1),
                item(arena, anna, "Brama z wyciągami", EquipmentCategory.CABLE, "CABLE_CROSSOVER", 1),
                item(arena, anna, "Sztanga olimpijska 20 kg", EquipmentCategory.FREE_WEIGHTS, "BARBELL", 4),
                item(arena, anna, "Hantle 2–40 kg", EquipmentCategory.FREE_WEIGHTS, "DUMBBELLS", null),
                item(arena, anna, "Power rack", EquipmentCategory.FREE_WEIGHTS, "SQUAT_RACK", 2),
                item(arena, jan, "Ławka płaska", EquipmentCategory.BENCH, "FLAT_BENCH", 3),
                item(arena, jan, "Ławka regulowana", EquipmentCategory.BENCH, "ADJUSTABLE_BENCH", 2),
                item(arena, jan, "Suwnica do nóg 45°", EquipmentCategory.STRENGTH_MACHINE, "LEG_PRESS", 1),
                item(arena, jan, "Maszyna do prostowania nóg", EquipmentCategory.STRENGTH_MACHINE, "LEG_EXTENSION",
                        1),
                item(arena, jan, "Maszyna do uginania nóg leżąc", EquipmentCategory.STRENGTH_MACHINE, "LEG_CURL",
                        1),
                item(arena, anna, "Butterfly", EquipmentCategory.STRENGTH_MACHINE, "PEC_DECK", 1),
                item(arena, anna, "Bieżnia", EquipmentCategory.CARDIO, "TREADMILL", 8),
                item(arena, anna, "Drążek do podciągania", EquipmentCategory.FUNCTIONAL, "PULL_UP_BAR", 2)));
        Equipment smith = item(arena, jan, "Suwnica Smitha", EquipmentCategory.STRENGTH_MACHINE, "SMITH_MACHINE", 1);
        smith.setStatus(EquipmentStatus.REMOVED_FROM_GYM);
        arenaEquipment.add(smith);

        List<Equipment> ironEquipment = List.of(
                item(iron, jan, "Sztanga 20 kg", EquipmentCategory.FREE_WEIGHTS, "BARBELL", 6),
                item(iron, jan, "Klatka do przysiadów", EquipmentCategory.FREE_WEIGHTS, "SQUAT_RACK", 3),
                item(iron, jan, "Ławka płaska", EquipmentCategory.BENCH, "FLAT_BENCH", 3),
                item(iron, jan, "Hantle 5–50 kg", EquipmentCategory.FREE_WEIGHTS, "DUMBBELLS", null),
                item(iron, jan, "Poręcze do dipów", EquipmentCategory.FUNCTIONAL, "DIP_STATION", 1),
                item(iron, jan, "Ławka rzymska", EquipmentCategory.BENCH, "ROMAN_CHAIR", 1));

        for (Equipment e : concat(arenaEquipment, ironEquipment)) {
            equipment.save(e);
            equipmentChanges.save(new EquipmentChange(e, e.getCreatedBy(), ChangeType.CREATED, Map.of()));
        }

        // Przykładowe plany. Pozycja na „Suwnicy Smitha” pokazuje ostrzeżenie o usuniętym sprzęcie.
        WorkoutPlan fbw = new WorkoutPlan(anna, arena, "FBW – 2 dni", "Trening całego ciała dwa razy w tygodniu.");
        PlanDay a = fbw.addDay("Dzień A");
        a.addItem(values("Przysiad ze sztangą", find(arenaEquipment, "Power rack"), 4, 6, 8, "60", 150));
        a.addItem(values("Wyciskanie sztangi na ławce płaskiej", find(arenaEquipment, "Sztanga olimpijska 20 kg"), 4,
                6, 8, "50", 150));
        a.addItem(values("Ściąganie drążka wyciągu górnego do klatki", find(arenaEquipment, "Wyciąg górny"), 3, 10,
                12, "45", 90));
        a.addItem(values("Deska (plank)", null, 3, 1, 1, null, 60));
        PlanDay b = fbw.addDay("Dzień B");
        b.addItem(values("Przysiad na suwnicy Smitha", smith, 3, 8, 10, "40", 120));
        b.addItem(values("Wyciskanie hantli na ławce skośnej", find(arenaEquipment, "Hantle 2–40 kg"), 3, 8, 12, "18",
                90));
        b.addItem(values("Wiosłowanie na wyciągu dolnym", find(arenaEquipment, "Wyciąg dolny do wiosłowania"), 3, 10,
                12, "50", 90));
        b.addItem(values("Pompki", null, 3, 10, 15, null, 60));
        plans.save(fbw);

        WorkoutPlan sbd = new WorkoutPlan(jan, iron, "Trójbój – podstawa", null);
        PlanDay heavy = sbd.addDay("Ciężki dzień");
        heavy.addItem(values("Przysiad ze sztangą", find(ironEquipment, "Klatka do przysiadów"), 5, 5, 5, "100", 180));
        heavy.addItem(values("Wyciskanie sztangi na ławce płaskiej", find(ironEquipment, "Sztanga 20 kg"), 5, 5, 5,
                "80", 180));
        heavy.addItem(values("Martwy ciąg", find(ironEquipment, "Sztanga 20 kg"), 3, 5, 5, "120", 240));
        plans.save(sbd);

        log.info("Dev data seeded: users {} / {} (password: {})", ANNA, JAN, PASSWORD);
    }

    private Equipment item(Gym gym, User author, String name, EquipmentCategory category, String typeCode,
            Integer quantity) {
        Equipment e = new Equipment(gym, name, category, author);
        e.setEquipmentType(equipmentTypes.findByCode(typeCode).orElseThrow());
        e.setQuantity(quantity);
        return e;
    }

    private PlanItem.Values values(String exerciseName, Equipment item, int sets, int repsMin, int repsMax,
            String weight, int rest) {
        return new PlanItem.Values(exercises.findFirstByNameAndScope(exerciseName, ExerciseScope.GLOBAL).orElseThrow(),
                item, sets, repsMin, repsMax, weight == null ? null : new BigDecimal(weight), rest, null);
    }

    private static Equipment find(List<Equipment> list, String name) {
        return list.stream().filter(e -> e.getName().equals(name)).findFirst().orElseThrow();
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        List<T> result = new ArrayList<>(a);
        result.addAll(b);
        return result;
    }
}
