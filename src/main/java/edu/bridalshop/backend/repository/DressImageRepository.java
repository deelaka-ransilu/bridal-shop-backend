package edu.bridalshop.backend.repository;

import edu.bridalshop.backend.entity.DressImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DressImageRepository extends JpaRepository<DressImage, Integer> {

    List<DressImage> findAllByDress_DressIdOrderByDisplayOrderAsc(Integer dressId);

    Optional<DressImage> findByPublicId(String publicId);

    int countByDress_DressId(Integer dressId);

    Optional<DressImage> findByDress_DressIdAndIsPrimaryTrue(Integer dressId);
}