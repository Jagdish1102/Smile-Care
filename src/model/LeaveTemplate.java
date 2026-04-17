package model;

public class LeaveTemplate {

    private int id;
    private String templateName;
    private String content;
    private String createdAt;

    public LeaveTemplate() {}

    public LeaveTemplate(String templateName, String content, String createdAt) {
        this.templateName = templateName;
        this.content = content;
        this.createdAt = createdAt;
    }

    // ==================== GETTERS & SETTERS ====================

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public String getTemplateName() { return templateName; }

    public void setTemplateName(String templateName) {
        this.templateName = templateName != null ? templateName.trim() : "";
    }

    public String getContent() { return content; }

    public void setContent(String content) {
        this.content = content != null ? content : "";
    }

    public String getCreatedAt() { return createdAt; }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt != null ? createdAt : "";
    }
}