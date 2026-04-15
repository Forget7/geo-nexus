package com.geonexus.repository;

import com.geonexus.domain.TilesetInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TilesetRepository extends JpaRepository<TilesetInfo, String> {
}
