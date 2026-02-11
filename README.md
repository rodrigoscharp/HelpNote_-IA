# 🧠 HELPNOTE IA

<p align="center">
  <img src="assets/logo-helpnote-ia.png" alt="HELPNOTE IA Logo" width="220" />
</p>

Assistente inteligente de anotações para palestras, cursos e eventos técnicos, utilizando **IA** para transcrição, organização, extração de palavras-chave e complementação inteligente de conteúdos automaticamente.

Projeto desenvolvido com foco em **portfolio profissional** e **uso real**, demonstrando integração entre **Java + Spring Boot + Inteligência Artificial**.

---

## 🎯 Objetivo do Projeto

Facilitar a vida de estudantes, desenvolvedores e profissionais que participam de palestras e cursos, resolvendo problemas como:

* Dificuldade em anotar tudo
* Anotações desorganizadas
* Perda de insights importantes
* Falta de revisão pós-evento

O **NoteMind AI** transforma áudio em conhecimento estruturado.

---

## 🚀 Funcionalidades

### 🧩 Funcionalidade Inteligente de Palavras‑Chave e Complementação

A IA identifica **palavras‑chave relevantes** em cada anotação ou trecho transcrito e, a partir delas:

* Classifica o tema principal
* Enriquece o conteúdo automaticamente
* Gera um **parágrafo explicativo complementar** logo abaixo do tópico
* Mantém coerência com o contexto da palestra ou curso

Essa funcionalidade transforma anotações simples em **conteúdo didático e revisável**.

### ✅ MVP (Versão Inicial)

* Upload de áudio de palestras/cursos
* Transcrição automática (Speech-to-Text)
* Geração de resumo inteligente
* Organização por tópicos
* Exportação das anotações em **Markdown**

### 🔜 Funcionalidades Futuras

* Gravação de áudio em tempo real
* Anotações rápidas durante a palestra
* Geração de insights e perguntas
* Sugestão de conteúdos para estudo
* Histórico inteligente de aprendizado
* Integração com Notion / GitHub

---

## 🧠 Como a IA é Utilizada

A Inteligência Artificial atua em três camadas principais:

### 1️⃣ Transcrição de Áudio

Responsável por converter áudio em texto.

Entrada:

* Arquivo de áudio (.mp3, .wav)

Saída:

* Texto bruto transcrito

---

### 2️⃣ Processamento Inteligente

A IA analisa o texto transcrito para:

* Identificar tópicos principais
* Extrair **palavras‑chave relevantes**
* Classificar o contexto do conteúdo
* Resumir conteúdos extensos
* Destacar pontos importantes
* Criar estrutura lógica das anotações
  A IA analisa o texto transcrito para:
* Identificar tópicos principais
* Resumir conteúdos extensos
* Destacar pontos importantes
* Criar estrutura lógica das anotações

---

### 3️⃣ Geração de Conteúdo Estruturado

A partir da transcrição e das palavras‑chave extraídas, a IA gera:

* Resumo
* Lista de aprendizados
* Insights
* Perguntas para revisão
* **Parágrafos explicativos complementares para cada tópico**

Tudo organizado e pronto para estudo.
A partir da transcrição, a IA gera:

* Resumo
* Lista de aprendizados
* Insights
* Perguntas para revisão

Tudo organizado e pronto para estudo.

---

## 🏗️ Arquitetura do Projeto

```
Frontend (Web / Mobile)
        ↓
API REST - Spring Boot
        ↓
Serviço de IA
        ↓
Banco de Dados
```

---

## 🧩 Stack Tecnológica

### Backend

* **Java 17+**
* **Spring Boot**
* Spring Web
* Spring Data JPA
* Spring Validation

### Banco de Dados

* PostgreSQL

### Inteligência Artificial

* API de Speech-to-Text
* API de LLM (Large Language Model)

### Infraestrutura

* Docker
* Deploy em cloud (AWS / Railway / Render)

---

## 📂 Estrutura do Projeto

```
notemind-ai
 ├── controller
 ├── service
 ├── domain
 ├── repository
 ├── dto
 ├── config
 └── NotemindApplication.java
```

---

## 🔌 Integração com IA (Visão Técnica)

### Fluxo de IA

1. Usuário envia áudio
2. Backend salva o arquivo
3. Serviço de transcrição é chamado
4. Texto gerado é enviado para o modelo de linguagem
5. IA retorna resumo e estrutura
6. Dados são salvos no banco
7. Resposta é enviada ao frontend

---

## 📦 Exemplos de Endpoints

### Upload de áudio

```
POST /api/notes/upload
```

### Gerar resumo

```
POST /api/notes/{id}/summary
```

### Buscar anotações

```
GET /api/notes/{id}
```

---

## 🔐 Boas Práticas

* Separação de responsabilidades (Controller / Service)
* DTOs para comunicação externa
* Tratamento de exceções
* Logs para processos de IA
* Configuração de variáveis sensíveis via `.env`

---

## 📌 Roadmap

* [x] Definição do MVP

* [x] Extração de palavras‑chave com IA

* [x] Complementação automática de tópicos

* [ ] Implementar upload de áudio

* [ ] Integração Speech‑to‑Text

* [ ] Integração com LLM

* [ ] Geração de resumo

* [ ] Exportação Markdown

* [ ] Deploy

* [x] Definição do MVP

* [ ] Implementar upload de áudio

* [ ] Integração Speech-to-Text

* [ ] Integração com LLM

* [ ] Geração de resumo

* [ ] Exportação Markdown

* [ ] Deploy

---

## 👨‍💻 Autor

**Rodrigão**
Desenvolvedor | Java | IA | Backend

Projeto criado para estudo, portfolio e evolução profissional com foco em **IA aplicada a produtividade e aprendizado**.

---

## ⭐ Observação

Este projeto demonstra **integração real de IA em aplicações Java**, indo além de CRUDs tradicionais e focando em soluções modernas e escaláveis.
