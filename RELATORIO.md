# Relatório Técnico — Refatoração do SaaS

### Aluno: Davi Augusto Ferreira de Carvalho / 202403066

## 1. Diagnóstico do débito técnico

Linhas referentes ao código legado (commit 1).

| # | Linhas| Code smell| Impacto |
|---|---|---|---|
| 1 | L18–105 | **Long Method** e violação do **SRP**: preço, banco, pagamento e e-mail no mesmo método (CC = 18) | Qualquer mudança de regra mexe no código de I/O; alto risco de regressão e conflitos de merge |
| 2 | L18–20 | **Long Parameter List** (8 parâmetros) e **Primitive Obsession** | Argumentos do mesmo tipo podem ser trocados sem erro de compilação; novo campo quebra todos os chamadores |
| 3 | L29–57 | Dinheiro em `double` (**Primitive Obsession**) | Erros de ponto flutuante chegam ao banco e ao cliente (ex.: `26.909999999999997`) |
| 4 | L25 | `LocalDate.now()` estático (**dependência oculta do relógio**) | Testes não determinísticos; o cupom BLACKFRIDAY só pode ser testado em novembro |
| 5 | L14–16, L59 | **Dependência direta de I/O** (violação do DIP) e credenciais hard-coded | O CI precisaria de PostgreSQL para testar; a senha fica no Git |
| 6 | L89–96 | SMTP concreto no serviço e **código morto** (`send()` comentado) | O log diz "E-mail disparado", mas nada é enviado |
| 7 | L100–102 | `catch (Exception e)` **genérico** | Falhas de infraestrutura viram uma linha de log; o chamador não sabe que falhou |
| 8 | L31–53 | **Magic Numbers** (`0.15`, `0.70`, `30.0`, `0.02`...) | Regras de negócio sem nome, difíceis de localizar e alterar |

## 2. Métricas antes vs. depois

Complexidade medida com lizard e cobertura com JaCoCo.

| Métrica | Antes | Depois |
|---|---|---|
| Complexidade ciclomática do método principal | 18 | 3 |
| Linhas do método principal | 88 | 18 |
| Parâmetros do método principal | 8 | 1 (`BillingCommand`) |
| Maior complexidade entre todos os métodos | 18 | 6 |
| Classes | 1 | 20 |
| Dependências de I/O concreto no serviço | 5 | 0 |
| `catch (Exception)` genérico | 1 | 0 |
| Credenciais hard-coded | 3 | 0 |
| Testes automatizados | 0 | 45 |
| Cobertura de linhas (domínio + aplicação) | 0% | 99% |
| Infraestrutura necessária para testar | PostgreSQL + SMTP | nenhuma |

O comportamento do cálculo foi preservado. Um teste de caracterização (`GoldenMasterTest`) compara o código novo com uma cópia do cálculo legado em 23.328 combinações de entrada.

As mudanças observáveis foram intencionais:
- Os valores passam a ser arredondados para centavos.
- Falhas de banco agora lançam exceção, em vez de ser silenciadas.
- O e-mail passa a ser enviado de fato; no legado o `send()` estava comentado.
