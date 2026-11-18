package it.stockmanager.ordine;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdineRepository extends JpaRepository<Ordine, UUID> {

    List<Ordine> findByOwnerIdOrderByCreatoIlDesc(UUID ownerId);
}
