package JOO.jooshop.productManagement.entity.enums;

public enum Size {
    XSMALL("X-Small"),
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large"),
    XLARGE("X-Large"),
    FREE("Free");

    private String description;

    Size(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
