package com.rallymate.global.init;

import com.github.f4b6a3.ulid.UlidCreator;
import com.rallymate.global.crypto.PhoneCrypto;
import com.rallymate.user.entity.Gender;
import com.rallymate.user.entity.User;
import com.rallymate.user.entity.UserRole;
import com.rallymate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 애플리케이션 시작 시 더미 {@link User} 데이터를 초기화합니다.
 * <p>
 * - {@code users} 테이블에 데이터가 하나도 없을 때(count == 0)만 10건을 삽입합니다.
 * - 이미 존재하는 경우에는 동작을 스킵하여 중복 삽입을 방지합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DummyUserDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PhoneCrypto phoneCrypto;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long count = userRepository.count();
        if (count != 0) {
            log.info("DummyUserDataInitializer skipped. users.count={}", count);
            return;
        }

        List<User> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            // 하이픈 없는 형식: 010 + 8자리 숫자
            String phoneNumber = "010" + String.format("%08d", 10000000 + i).trim();

            String uid = UlidCreator.getUlid().toString();
            String phoneEnc = phoneCrypto.encrypt(phoneNumber);

            String phoneHash = phoneCrypto.hash(phoneNumber);
            log.info("phoneNumber: {}, phoneHash: {}", phoneNumber, phoneHash);

            Gender gender = (i % 3 == 0) ? Gender.HIDDEN : (i % 2 == 0 ? Gender.MALE : Gender.FEMALE);

            User user = User.of(
                    uid,
                    phoneEnc,
                    phoneHash,
                    "dummyUser" + (i + 1),
                    gender,
                    "https://example.com/profile/dummyUser" + (i + 1) + ".png",
                    "Tennis",
                    "Seoul",
                    UserRole.USER
            );
            users.add(user);
        }

        userRepository.saveAll(users);
        log.info("DummyUserDataInitializer inserted dummy users. insertedCount={}", users.size());
    }
}

