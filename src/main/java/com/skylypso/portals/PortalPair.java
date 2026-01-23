package com.skylypso.portals;

public record PortalPair(PortalLocation entry, PortalLocation exit)
{
    public static PortalPair empty()
    {
        return new PortalPair(PortalLocation.empty(), PortalLocation.empty());
    }

    public boolean isComplete()
    {
        return entry.isSet() && exit.isSet();
    }

    public String describe()
    {
        if (!entry.isSet() && !exit.isSet()) return "Portal pair not set.";
        return "Entry: " + entry + " | Exit: " + exit;
    }

    // Minimal JSON (no dependencies). Good enough for PoC.
    public String toJson()
    {
        return "{"
            + "\"entry\":" + entry.toJson() + ","
            + "\"exit\":" + exit.toJson()
            + "}";
    }

    public static PortalPair fromJson(String json)
    {
        // Tiny, fragile parser (PoC!). Replace with Gson/Jackson if you want.
        PortalLocation e = PortalLocation.fromJson(extractObject(json, "entry"));
        PortalLocation x = PortalLocation.fromJson(extractObject(json, "exit"));
        return new PortalPair(e, x);
    }

    private static String extractObject(String json, String key)
    {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return "{}";
        int colon = json.indexOf(':', i);
        int start = json.indexOf('{', colon);
        int depth = 0;
        for (int p = start; p < json.length(); p++)
        {
            char c = json.charAt(p);
            if (c == '{') depth++;
            if (c == '}') depth--;
            if (depth == 0) return json.substring(start, p + 1);
        }
        return "{}";
    }
}