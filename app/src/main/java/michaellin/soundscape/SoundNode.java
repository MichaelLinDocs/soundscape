package michaellin.soundscape;

public class SoundNode {
    SoundNode(double latitude, double longitude, double radius, String uriLink)
    {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.uriLink = uriLink;
    }

    private double longitude;
    private double latitude;
    private double radius;
    private String uriLink;

    public double getLongitude()
    {
        return longitude;
    }

    public double getLatitude()
    {
        return latitude;
    }

    public double getRadius()
    {
        return radius;
    }

    public String getUriLink()
    {
        return uriLink;
    }
}
