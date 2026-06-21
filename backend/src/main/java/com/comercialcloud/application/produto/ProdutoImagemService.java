package com.comercialcloud.application.produto;

import com.comercialcloud.domain.exception.BusinessException;
import com.comercialcloud.infrastructure.security.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
public class ProdutoImagemService {

    private static final Pattern EXTENSION = Pattern.compile("^(image/(jpeg|jpg|png|webp|gif))$");

    @Inject TenantContext tenantContext;

    @Inject ProdutoService produtoService;

    @ConfigProperty(name = "comercialcloud.upload.dir", defaultValue = "uploads/produtos")
    String uploadDir;

    public String salvarImagemBase64(UUID produtoId, String contentType, String conteudoBase64) {
        if (conteudoBase64 == null || conteudoBase64.isBlank()) {
            throw new BusinessException("IMAGEM_OBRIGATORIA", "Conteúdo da imagem é obrigatório");
        }
        String tipo = contentType != null ? contentType.toLowerCase(Locale.ROOT) : "image/jpeg";
        if (!EXTENSION.matcher(tipo).matches()) {
            throw new BusinessException("IMAGEM_INVALIDA", "Formato de imagem não suportado");
        }

        String payload = conteudoBase64;
        int comma = payload.indexOf(',');
        if (comma >= 0) {
            payload = payload.substring(comma + 1);
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(payload.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("IMAGEM_INVALIDA", "Conteúdo base64 inválido");
        }
        if (bytes.length == 0 || bytes.length > 5 * 1024 * 1024) {
            throw new BusinessException("IMAGEM_INVALIDA", "Imagem deve ter entre 1 byte e 5 MB");
        }

        String ext = tipo.contains("png") ? "png" : tipo.contains("webp") ? "webp" : tipo.contains("gif") ? "gif" : "jpg";
        UUID tenantId = tenantContext.getTenantId();
        produtoService.buscarPorId(produtoId);

        Path dir = Path.of(uploadDir, tenantId.toString());
        try {
            Files.createDirectories(dir);
            String fileName = produtoId + "-" + UUID.randomUUID() + "." + ext;
            Path target = dir.resolve(fileName);
            Files.write(target, bytes);
            String imagemUrl = "/api/v1/produtos/midias/" + tenantId + "/" + fileName;
            produtoService.atualizarImagemUrl(produtoId, imagemUrl);
            return imagemUrl;
        } catch (IOException e) {
            throw new BusinessException("IMAGEM_FALHA", "Não foi possível salvar a imagem");
        }
    }

    public byte[] lerImagem(UUID tenantId, String fileName) {
        if (fileName == null || fileName.contains("..") || fileName.contains("/")) {
            throw new BusinessException("IMAGEM_INVALIDA", "Arquivo inválido");
        }
        Path path = Path.of(uploadDir, tenantId.toString(), fileName);
        if (!Files.exists(path)) {
            throw new BusinessException("IMAGEM_NAO_ENCONTRADA", "Imagem não encontrada");
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new BusinessException("IMAGEM_FALHA", "Não foi possível ler a imagem");
        }
    }

    public String contentTypeDeArquivo(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }
}
