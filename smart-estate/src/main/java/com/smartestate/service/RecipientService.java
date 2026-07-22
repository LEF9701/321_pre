package com.smartestate.service;

import com.smartestate.model.Role;
import com.smartestate.model.User;
import com.smartestate.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Resolves the set of resident recipients for a notification scope (FR-1.5 batch filtering). */
@Service
public class RecipientService {

    private final UserRepository userRepository;

    public RecipientService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> all() {
        return userRepository.findByRole(Role.RESIDENT).stream()
                .filter(u -> u.getUnit() != null)
                .toList();
    }

    public List<User> forBuilding(Long buildingId) {
        return all().stream()
                .filter(u -> buildingId.equals(buildingIdOf(u)))
                .toList();
    }

    public List<User> forBlock(Long blockId) {
        return all().stream()
                .filter(u -> blockId.equals(blockIdOf(u)))
                .toList();
    }

    public List<User> resolve(String scopeType, Long scopeId) {
        if (scopeType == null) return all();
        return switch (scopeType) {
            case "BUILDING" -> scopeId == null ? all() : forBuilding(scopeId);
            case "BLOCK" -> scopeId == null ? all() : forBlock(scopeId);
            default -> all();
        };
    }

    private Long buildingIdOf(User u) {
        try {
            return u.getUnit().getFloor().getBlock().getBuilding().getId();
        } catch (Exception e) {
            return null;
        }
    }

    private Long blockIdOf(User u) {
        try {
            return u.getUnit().getFloor().getBlock().getId();
        } catch (Exception e) {
            return null;
        }
    }
}
