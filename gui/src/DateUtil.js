class DateUtil {
    static formatTime(millis) {
        const seconds = millis/1000;
        const h = Math.floor(seconds / 3600);
        const m = Math.floor((seconds % 3600) / 60);
        const s = Math.round(seconds % 60);
        if (h>0){
            return `${h}h ${m > 9 ? m: "0"+m}m`
        } else if (m>0) {
            return `${m}m ${s > 9 ? s: "0"+s}s`
        } else {
            return `${s}s`
        }
    }
}
export default DateUtil

