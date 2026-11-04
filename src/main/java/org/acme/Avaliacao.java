package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Avaliacao extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String descricao;
    public Integer nota;
    public Boolean urgencia;
    public LocalDateTime dataEnvio;

    // Construtor padrão (necessário para o JPA e Jackson)
    public Avaliacao() {}

    // Construtor com parâmetros (opcional, para testes)
    public Avaliacao(String descricao, Integer nota) {
        this.descricao = descricao;
        this.nota = nota;
    }

    // Método executado automaticamente antes de salvar no banco
    @PrePersist
    public void prePersist() {
        // Define a data de envio se não estiver preenchida
        if (this.dataEnvio == null) {
            this.dataEnvio = LocalDateTime.now();
        }
        // Calcula a urgência se não estiver preenchida
        if (this.urgencia == null && this.nota != null) {
            this.urgencia = this.nota < 3;
        }
    }
}
