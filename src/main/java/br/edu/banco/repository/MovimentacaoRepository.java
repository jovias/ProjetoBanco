package br.edu.banco.repository;

import br.edu.banco.model.Movimentacao;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {
    List<Movimentacao> findByOrigemIdOrDestinoIdOrderByIdDesc(
            Long origemId, Long destinoId, Pageable pagina);
}
