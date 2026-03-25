package edu.bridalshop.backend.repository;

import edu.bridalshop.backend.entity.DressFulfillmentOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DressFulfillmentOptionRepository extends JpaRepository<DressFulfillmentOption, Integer> {

    List<DressFulfillmentOption> findAllByDress_DressId(Integer dressId);

    Optional<DressFulfillmentOption> findByDress_DressIdAndFulfillmentType(
            Integer dressId, String fulfillmentType);

    int countByDress_DressId(Integer dressId);
}