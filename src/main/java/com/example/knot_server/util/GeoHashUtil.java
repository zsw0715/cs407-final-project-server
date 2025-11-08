package com.example.knot_server.util;

/**
 * GeoHash工具类
 * 将经纬度编码为GeoHash字符串，用于地理位置索引和查询
 * 
 * 精度参考：
 * - 5位: ±2.4km
 * - 6位: ±0.61km
 * - 7位: ±0.076km (76m)
 * - 8位: ±0.019km (19m)
 */
public class GeoHashUtil {

    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

    /**
     * 将经纬度编码为GeoHash
     * 
     * @param latitude  纬度 (-90, 90)
     * @param longitude 经度 (-180, 180)
     * @param precision 精度位数（建议6-8）
     * @return GeoHash字符串
     */
    public static String encode(double latitude, double longitude, int precision) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        if (precision < 1 || precision > 12) {
            throw new IllegalArgumentException("Precision must be between 1 and 12");
        }

        double[] latRange = { -90.0, 90.0 };
        double[] lonRange = { -180.0, 180.0 };
        StringBuilder geohash = new StringBuilder();
        boolean isEven = true;
        int bit = 0;
        int ch = 0;

        while (geohash.length() < precision) {
            double mid;
            if (isEven) {
                // 处理经度
                mid = (lonRange[0] + lonRange[1]) / 2;
                if (longitude > mid) {
                    ch |= (1 << (4 - bit));
                    lonRange[0] = mid;
                } else {
                    lonRange[1] = mid;
                }
            } else {
                // 处理纬度
                mid = (latRange[0] + latRange[1]) / 2;
                if (latitude > mid) {
                    ch |= (1 << (4 - bit));
                    latRange[0] = mid;
                } else {
                    latRange[1] = mid;
                }
            }
            isEven = !isEven;

            if (bit < 4) {
                bit++;
            } else {
                geohash.append(BASE32.charAt(ch));
                bit = 0;
                ch = 0;
            }
        }

        return geohash.toString();
    }

    /**
     * 使用Haversine公式计算两点间的距离
     * 
     * @param lat1 起点纬度
     * @param lng1 起点经度
     * @param lat2 终点纬度
     * @param lng2 终点经度
     * @return 距离（米）
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int EARTH_RADIUS = 6371000; // 地球半径（米）

        // 将角度转换为弧度
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);

        // Haversine公式
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                        * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c; // 返回距离（米）
    }

}
