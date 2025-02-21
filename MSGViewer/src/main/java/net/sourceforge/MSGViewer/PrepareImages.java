package net.sourceforge.MSGViewer;

import com.auxilii.msgparser.Message;
import com.auxilii.msgparser.attachment.Attachment;
import com.auxilii.msgparser.attachment.FileAttachment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrepareImages {

    private static final Logger logger = LogManager.getLogger(PrepareImages.class);
    private static final String ATTRIBUTES_GROUP_NAME = "attr";
    private static final Pattern IMG_PATTERN = Pattern.compile("<\\s*img\\s+(?<" + ATTRIBUTES_GROUP_NAME + ">[^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final String SRC_GROUP_NAME = "src";
    private static final Pattern SRC_PATTERN = Pattern.compile("src\\s*=\\s*\"(?<" + SRC_GROUP_NAME + ">[^\"]*)\"", Pattern.CASE_INSENSITIVE);

    private final ViewerHelper fileRepository;
    private final Map<String, FileAttachment> attachmentById = new HashMap<>();
    private final Map<String, FileAttachment> attachmentByLocation = new HashMap<>();

    public PrepareImages(ViewerHelper fileRepository, Message message) {
        this.fileRepository = fileRepository;

        for (Attachment att : message.getAttachments()) {
            if (att instanceof FileAttachment) {
                FileAttachment fatt = (FileAttachment) att;
                if (fatt.getContentId() != null) {
                    attachmentById.put(fatt.getContentId(), fatt);
                }
                if (fatt.getContentLocation() != null) {
                    attachmentByLocation.put(fatt.getContentLocation(), fatt);
                }
            }
        }
    }

    public String prepareImages(String s) {
        Matcher matcher = IMG_PATTERN.matcher(s);
        StringBuilder ret = new StringBuilder();
        int lastMatchEnd = 0;
        for (; matcher.find(lastMatchEnd); lastMatchEnd = matcher.end(ATTRIBUTES_GROUP_NAME)) {
            try {
                ret.append(s, lastMatchEnd, matcher.start(ATTRIBUTES_GROUP_NAME));
                String img = matcher.group(ATTRIBUTES_GROUP_NAME);
                ret.append(replace_src(img));
            } catch (RuntimeException rex) {
                logger.error("Failed parsing image tag :", rex);
            }
        }
        ret.append(s.substring(lastMatchEnd));
        return ret.toString();
    }

    private String replace_src(String s) {
        Matcher matcher = SRC_PATTERN.matcher(s);
        StringBuilder ret = new StringBuilder();
        int lastMatchEnd = 0;
        for (; matcher.find(lastMatchEnd); lastMatchEnd = matcher.end(SRC_GROUP_NAME)) {
            ret.append(s, lastMatchEnd, matcher.start(SRC_GROUP_NAME));
            String src = matcher.group(SRC_GROUP_NAME);
            ret.append(getImgsrc(src));
        }
        ret.append(s.substring(lastMatchEnd));
        return ret.toString();
    }

    private URI getImgsrc(String src) {
        URI imgsrc = URI.create(src);
        if (!imgsrc.isAbsolute()) {
            FileAttachment fatt = attachmentByLocation.remove(imgsrc.getPath());
            return fileRepository.getTempFile(fatt).toURI();
        }
        if (imgsrc.getScheme().equals("cid")) {
            FileAttachment fatt = attachmentById.remove(imgsrc.getSchemeSpecificPart());
            return fileRepository.getTempFile(fatt).toURI();
        }
        return imgsrc;
    }
}
