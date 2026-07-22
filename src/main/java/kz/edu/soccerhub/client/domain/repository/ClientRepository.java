package kz.edu.soccerhub.client.domain.repository;

import kz.edu.soccerhub.client.domain.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByPhone(String phone);

    Optional<Client> findByUserId(UUID userId);

    @Query("""
            select client
            from Client client
            where client.branchId = :branchId
              and (
                    :searchText is null
                    or lower(trim(concat(coalesce(client.firstName, ''), concat(' ', coalesce(client.lastName, ''))))) like :searchText
                    or lower(coalesce(client.phone, '')) like :searchText
                    or lower(coalesce(client.email, '')) like :searchText
                    or (:searchId is not null and client.id = :searchId)
              )
            """)
    Page<Client> search(UUID branchId, String searchText, UUID searchId, Pageable pageable);
}
