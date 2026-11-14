package br.edu.banco.repository;

import br.edu.banco.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaRepository extends JpaRepository<Conta, Long> {
    boolean existsByNumero(String numero);
}
