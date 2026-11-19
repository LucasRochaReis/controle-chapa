# Controle de Chapa

Sistema web para controle da produção de chapas (impressão offset): registro de
lançamentos de produção e de ocorrências (defeitos), relatórios gerenciais com
exportação para Excel e cadastros auxiliares.

Projeto de portfólio em **Java + Spring Boot**, com autenticação, controle de
perfil de acesso, migrações de banco versionadas e interface server-side
(Thymeleaf).

---

## Funcionalidades

- **Autenticação e autorização** com Spring Security (perfis `ADMIN`, `GESTOR`,
  `VISUALIZADOR`, `USUARIO`) e troca de senha obrigatória no primeiro acesso.
- **Lançamentos de produção** de chapas (data, turno, OP, cliente, quantidade,
  máquina, operador, chapa, CTP e observação).
- **Ocorrências** (defeitos) com perda, código de ocorrência, setor e observação.
- **Relatórios**: dashboard, resumo geral e controle mensal, com exportação para
  Excel (Apache POI).
- **Cadastros (lookups)**: turno, máquina, chapa, CTP, código de ocorrência e
  operador; além do cadastro de clientes.
- **Importação de lançamentos via Excel**.
- **Usabilidade**: autocomplete de cliente (com exclusão em linha) e sugestão de
  observações já lançadas.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.3.4 |
| Web / View | Spring MVC + Thymeleaf (+ Thymeleaf Extras SpringSecurity 6) |
| Persistência | Spring Data JPA / Hibernate 6 |
| Migrações | Flyway |
| Banco | PostgreSQL |
| Segurança | Spring Security 6 (form login, BCrypt, CSRF, remember-me) |
| Excel | Apache POI 5.2.5 |
| Front-end | Bootstrap 5.3.3 (CDN) + CSS/JS próprios |
| Build | Maven |

## Pré-requisitos

- JDK 17 ou superior
- Maven 3.9+
- PostgreSQL 14+

## Setup

### 1. Banco de dados

Crie o banco (as tabelas são criadas automaticamente pelo Flyway na primeira
execução):

```sql
CREATE DATABASE controle_chapa;
```

### 2. Variáveis de ambiente

O projeto **não** possui credenciais no código: a senha do banco é obrigatória.

```bash
# Linux/macOS
export DB_PASSWORD=sua-senha
export DB_URL=jdbc:postgresql://localhost:5432/controle_chapa
export DB_USER=postgres

# Windows PowerShell
$env:DB_PASSWORD="sua-senha"
```

Há um modelo em [.env.example](.env.example) (sem valores reais). O Spring Boot
**não** carrega `.env` automaticamente — use as variáveis do seu sistema/IDE.

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `DB_PASSWORD` | sim | — | Senha do PostgreSQL |
| `DB_URL` | não | `jdbc:postgresql://localhost:5432/controle_chapa` | URL JDBC |
| `DB_USER` | não | `postgres` | Usuário do banco |
| `SERVER_PORT` | não | `6001` | Porta HTTP |
| `SERVER_CONTEXT_PATH` | não | `/controle_chapa` | Context path |
| `APP_ADMIN_USERNAME` | não | `admin` | Usuário admin criado no boot |
| `APP_ADMIN_PASSWORD` | não | `admin123` | Senha do admin criado no boot |

### 3. Executar

```bash
mvn spring-boot:run
```

Acesse: <http://localhost:6001/controle_chapa>

Login inicial: usuário definido em `APP_ADMIN_USERNAME` (padrão `admin`).

### 4. Testes

```bash
mvn test
```

Os testes rodam com **H2 em memória**, sem depender do PostgreSQL. No perfil de
teste o **Flyway é desativado** (as migrações usam SQL específico do PostgreSQL,
como `ON CONFLICT`) e o schema é criado pelo Hibernate (`ddl-auto=create-drop`).

## Estrutura de pastas

```
src
├── main
│   ├── java/br/com/techfiles/controlepapel
│   │   ├── config/    # segurança, seeds, configuração MVC
│   │   ├── model/     # entidades e enums de domínio
│   │   ├── repo/      # repositórios Spring Data JPA
│   │   ├── service/   # regras de negócio
│   │   └── web/       # controllers, formulários e DTOs
│   └── resources
│       ├── db/paper-migration/   # migrações Flyway (V1..V15)
│       ├── static/               # app.css, app.js
│       ├── templates/            # páginas Thymeleaf
│       └── application.properties
└── test               # testes automatizados (H2)
```

## Segurança e limitações conhecidas

- Senhas de usuário são armazenadas com **BCrypt**; autenticação via formulário
  com **CSRF** habilitado.
- Nenhuma credencial é versionada: a senha do banco vem de `DB_PASSWORD` e a do
  admin inicial de `APP_ADMIN_USERNAME`/`APP_ADMIN_PASSWORD`. **Troque a senha
  padrão do admin em produção.**
- **Limitações (projeto de demonstração):**
  - A chave do *remember-me* está fixa no código (`SecurityConfig`); ideal é
    externalizá-la e mantê-la secreta.
  - Não há configuração de HTTPS — espera-se execução atrás de um proxy reverso.
  - O log de SQL (`format_sql`) está habilitado para facilitar o estudo do
    projeto.
- Segredos, `target/`, logs, `.env` e pastas de upload/IDE estão cobertos pelo
  [.gitignore](.gitignore).
