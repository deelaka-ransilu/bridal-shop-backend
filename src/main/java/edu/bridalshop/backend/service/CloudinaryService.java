package edu.bridalshop.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder}")
    private String baseFolder;

    // ── Upload profile picture — returns public URL ────────────────────
    public String uploadProfilePicture(MultipartFile file,
                                       String publicId) throws IOException {
        log.debug("Uploading profile picture for publicId: {}", publicId);
        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder",         baseFolder + "/profiles",
                            "public_id",      publicId,
                            "overwrite",      true,
                            "resource_type",  "image",
                            "transformation", "w_400,h_400,c_fill,g_face,q_auto,f_auto"
                    )
            );
            String url = (String) uploadResult.get("secure_url");
            log.info("Profile picture uploaded successfully: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Failed to upload profile picture for publicId: {}", publicId, e);
            throw e;
        }
    }

    // ── Upload dress image — returns public URL ────────────────────────
    public String uploadDressImage(MultipartFile file) throws IOException {
        log.debug("Uploading dress image");
        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder",        baseFolder + "/dresses",
                            "resource_type", "image",
                            "transformation","q_auto,f_auto"
                    )
            );
            String url = (String) uploadResult.get("secure_url");
            log.info("Dress image uploaded successfully: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Failed to upload dress image", e);
            throw e;
        }
    }

    // ── Delete a file by public_id ─────────────────────────────────────
    public void deleteFile(String cloudinaryPublicId) throws Exception {
        log.debug("Deleting file from Cloudinary: {}", cloudinaryPublicId);
        try {
            cloudinary.uploader().destroy(cloudinaryPublicId,
                    ObjectUtils.emptyMap());
            log.info("File deleted successfully: {}", cloudinaryPublicId);
        } catch (Exception e) {
            log.error("Failed to delete file: {}", cloudinaryPublicId, e);
            throw e;
        }
    }
}