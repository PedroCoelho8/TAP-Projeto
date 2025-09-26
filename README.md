# TAP-Projeto — Advanced Programming Techniques (Functional Programming)

## Contexto

Este repositório corresponde ao projeto prático da unidade curricular de **Técnicas Avançadas de Programação** (TAP), do Mestrado em Engenharia Informática (ISEP, 2024/2025). O objetivo central é conceber e desenvolver uma aplicação usando técnicas de programação funcional, para resolver o problema do agendamento de ordens de produção numa fábrica.

## Objetivos

- Analisar o problema de scheduling de ordens de produção com múltiplos recursos e restrições.
- Conceber algoritmos de scheduling utilizando técnicas de programação funcional (Scala), sem mutabilidade.
- Desenvolver testes unitários, funcionais e baseados em propriedades.
- Gerar artefactos de input/output em XML, validados por esquemas XSD.
- Documentar as decisões técnicas e justificações.

## Descrição do Problema

O sistema deve permitir:
- Definir ordens de produção (cada uma para um produto e quantidade).
- Modelar produtos como sequências lineares de tarefas.
- Descrever tarefas (tempo, recursos físicos necessários).
- Gerir recursos físicos (máquinas, postos) e humanos (habilidades específicas).
- Agendar todas as tarefas, garantindo a correta alocação/extensão dos recursos.
- Persistir o agendamento em disco, em ficheiro XML.

## Milestones e Entregáveis

### Milestone 1 — MVP (Agendamento Ingénuo)
- Agendamento sequencial: apenas uma tarefa em execução de cada vez.
- Geração de ficheiro XML de agendamento.
- Implementação de testes unitários e funcionais.
- **Entregáveis**: Código, ficheiros XML de teste, sumário executivo.

### Milestone 2 — Testes Baseados em Propriedades
- Definição e implementação de propriedades do domínio (ex: recursos não são partilhados simultaneamente, todos os pedidos são agendados).
- Testes usando ScalaCheck.
- **Entregáveis**: Código, sumário executivo.

### Milestone 3 — Otimização de Produção
- Agendamento otimizado (tarefas paralelas, ordem de produção otimizada).
- Respeito integral pelas restrições de recursos.
- Novo conjunto de ficheiros XML de teste.
- **Entregáveis**: Código, ficheiros XML de teste, relatório de projeto.

## Requisitos Técnicos

- **Linguagem:** Scala (programação funcional estrita, sem mutabilidade)
- **Testes:** ScalaTest, ScalaCheck (unitários, funcionais, propriedades)
- **XML:** Importação/exportação usando `scala-xml`, com schemas fornecidos (`production.xsd`, `schedule.xsd`)
- **Cobertura:** Plugin scoverage para análise de cobertura de testes
- **Execução:** Algoritmos devem aceitar qualquer ficheiro XML de input compatível
- **Regras:** Não alterar pastas de avaliação automática

## Organização do Repositório

- `src/` — Código-fonte principal (Scala)
- `files/assessment/` — Ficheiros de teste e exemplos (NÃO ALTERAR onde indicado)
- `test/` — Testes unitários, funcionais e de propriedade
- `docs/` — Sumário executivo, relatório de projeto, documentação técnica
- `README.md` — Este ficheiro

## Documentação

- **Sumário executivo:** Decisões principais, alternativas, justificações, melhorias futuras.
- **Relatório de projeto:** Análise dos algoritmos, propriedades, uso de programação funcional, limitações.

## Avaliação

- Avaliação automática via comandos sbt.
- Entregas parciais e finais para cada milestone, com feedback contínuo.
- Penalização severa para mutabilidade ou plágio.

## Como executar os testes automáticos

```bash
# Avaliação MS01
sbt "testOnly pj.assessment.AssessmentTestMS01"

# Avaliação MS03
sbt "testOnly pj.assessment.AssessmentTestMS03"
```


> **Técnicas Avançadas de Programação — Mestrado em Engenharia Informática, ISEP 2024/2025**
