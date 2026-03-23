package edu.bridalshop.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder}")
    private String baseFolder;

    // ── Upload profile picture — returns public URL ────────────────────
    public String uploadProfilePicture(MultipartFile file,
                                       String publicId) throws IOException {
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
        return (String) uploadResult.get("secure_url");
    }

    // ── Upload dress image — returns public URL ────────────────────────
    public String uploadDressImage(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder",        baseFolder + "/dresses",
                        "resource_type", "image",
                        "transformation","q_auto,f_auto"
                )
        );
        return (String) uploadResult.get("secure_url");
    }

    // ── Delete a file by public_id ─────────────────────────────────────
    public void deleteFile(String cloudinaryPublicId) throws Exception {
        cloudinary.uploader().destroy(cloudinaryPublicId,
                ObjectUtils.emptyMap());
    }
}