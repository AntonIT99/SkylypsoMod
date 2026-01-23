package com.skylypso.portals;

public record PortalLocation(String world, int x, int y, int z)
{
    public static PortalLocation empty()
    {
        return new PortalLocation("", 0, 0, 0);
    }

    public boolean isSet()
    {
        return world != null && !world.isBlank();
    }

    public boolean matches(String worldName, int bx, int by, int bz)
    {
        return isSet()
            && world.equals(worldName)
            && x == bx && y == by && z == bz;
    }

    @Override
    public String toString()
    {
        return (isSet() ? world : "<unset>") + " @ " + x + "," + y + "," + z;
    }

    public String toJson()
    {
        return "{"
            + "\"world\":\"" + escape(world) + "\","
            + "\"x\":" + x + ","
            + "\"y\":" + y + ","
            + "\"z\":" + z
            + "}";
    }

    public static PortalLocation fromJson(String obj)
    {
        String w = readString(obj, "world");
        int x = readInt(obj, "x");
        int y = readInt(obj, "y");
        int z = readInt(obj, "z");
        if (w == null) w = "";
        return new PortalLocation(w, x, y, z);
    }

    private static String escape(String s)
    {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String readString(String obj, String key)
    {
        int i = obj.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        int colon = obj.indexOf(':', i);
        int q1 = obj.indexOf('"', colon + 1);
        int q2 = obj.indexOf('"', q1 + 1);
        if (q1 < 0 || q2 < 0) return null;
        return obj.substring(q1 + 1, q2);
    }

    private static int readInt(String obj, String key)
    {
        int i = obj.indexOf("\"" + key + "\"");
        if (i < 0) return 0;
        int colon = obj.indexOf(':', i);
        int end = colon + 1;
        while (end < obj.length() && "0123456789-".indexOf(obj.charAt(end)) >= 0) end++;
        String num = obj.substring(colon + 1, end).trim();
        try
        {
            return Integer.parseInt(num);
        }
        catch (Exception e)
        {
            return 0;
        }
    }
}