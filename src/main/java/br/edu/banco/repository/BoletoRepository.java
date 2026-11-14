package br.edu.banco.repository;

import br.edu.banco.model.Boleto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoletoRepository extends JpaRepository<Boleto, Long> {
    boolean existsByCodigo(String codigo);
}
