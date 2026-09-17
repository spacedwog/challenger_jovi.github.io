# Java Upload API

Backend em Java/Spring Boot para integrar o protótipo SmartShot JOVI com upload real de imagens.

## Como executar

```bash
cd java-upload-api
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`.

## Endpoints

- `GET /api/uploads/health` — status da API
- `POST /api/uploads` — upload multipart de imagem com metadados de captura
- `POST /api/uploads/{mediaId}/gallery?userId=1` — vincula a mídia à Daily Gallery
- `POST /api/uploads/{mediaId}/share?platform=WhatsApp` — simula compartilhamento
- `GET /api/uploads/{mediaId}/file` — retorna a imagem enviada
- `GET /api/uploads` — lista uploads em memória

## Campos enviados pelo protótipo

- `file`
- `userId`
- `mode`
- `quality`
- `captureType`

## Observações

- Apenas imagens são aceitas.
- Os arquivos são salvos no diretório temporário local.
- O estado de uploads e gallery é mantido em memória para a demonstração.
