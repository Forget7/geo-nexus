package com.geonexus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 军标符号服务 - Military Symbol (MIL-STD-2525)
 */
@Slf4j
@Service
public class MilitarySymbolService {
    
    private final CacheService cacheService;
    private final Map<String, MilitarySymbol> symbols = new ConcurrentHashMap<>();
    private final Map<String, SymbolCategory> categories = new ConcurrentHashMap<>();
    
    public MilitarySymbolService(CacheService cacheService) {
        this.cacheService = cacheService;
        initializeSymbolLibrary();
    }
    
    private void initializeSymbolLibrary() {
        // 陆军
        createCategory(SymbolCategory.builder().id("ground-unit").name("陆军单位").code("S").description("Ground Unit Symbols").build());
        addSymbol(MilitarySymbol.builder().id("infantry").name("步兵").categoryId("ground-unit").sidc("S*G*G*GPS--").affiliation("friend").symbolType("unit").hierarchy(" infantry").build());
        addSymbol(MilitarySymbol.builder().id("armor").name("装甲").categoryId("ground-unit").sidc("S*G*A*GPS--").affiliation("friend").symbolType("unit").hierarchy(" armor").build());
        addSymbol(MilitarySymbol.builder().id("mechanized").name("机械化步兵").categoryId("ground-unit").sidc("S*G*M*GPS--").affiliation("friend").symbolType("unit").hierarchy(" mechanized infantry").build());
        addSymbol(MilitarySymbol.builder().id("artillery").name("炮兵").categoryId("ground-unit").sidc("S*G*F*GPS--").affiliation("friend").symbolType("unit").hierarchy(" artillery").build());
        addSymbol(MilitarySymbol.builder().id("tank").name("坦克").categoryId("ground-unit").sidc("S*G*T*GPS--").affiliation("friend").symbolType("unit").hierarchy(" tank").build());
        addSymbol(MilitarySymbol.builder().id("reconnaissance").name("侦察").categoryId("ground-unit").sidc("S*G*E*GPS--").affiliation("friend").symbolType("unit").hierarchy(" reconnaissance").build());
        addSymbol(MilitarySymbol.builder().id("engineer").name("工程兵").categoryId("ground-unit").sidc("S*G*S*GPS--").affiliation("friend").symbolType("unit").hierarchy(" engineer").build());
        addSymbol(MilitarySymbol.builder().id("air-defense").name("防空").categoryId("ground-unit").sidc("S*G*D*GPS--").affiliation("friend").symbolType("unit").hierarchy(" air defense").build());
        addSymbol(MilitarySymbol.builder().id("helicopter").name("直升机").categoryId("ground-unit").sidc("S*G*H*GPS--").affiliation("friend").symbolType("unit").hierarchy(" helicopter").build());
        
        // 海军
        createCategory(SymbolCategory.builder().id("naval-unit").name("海军单位").code("N").description("Naval Unit Symbols").build());
        addSymbol(MilitarySymbol.builder().id("naval-combat").name("作战舰艇").categoryId("naval-unit").sidc("N*N*C*NA---").affiliation("friend").symbolType("unit").hierarchy(" combatant").build());
        addSymbol(MilitarySymbol.builder().id("aircraft-carrier").name("航空母舰").categoryId("naval-unit").sidc("N*N*C*CA---").affiliation("friend").symbolType("unit").hierarchy(" aircraft carrier").build());
        addSymbol(MilitarySymbol.builder().id("submarine").name("潜艇").categoryId("naval-unit").sidc("N*N*S*SS---").affiliation("friend").symbolType("unit").hierarchy(" submarine").build());
        addSymbol(MilitarySymbol.builder().id("amphibious").name("两栖舰艇").categoryId("naval-unit").sidc("N*N*L*LS---").affiliation("friend").symbolType("unit").hierarchy(" amphibious").build());
        
        // 空军
        createCategory(SymbolCategory.builder().id("air-unit").name("空军单位").code("A").description("Air Unit Symbols").build());
        addSymbol(MilitarySymbol.builder().id("air-fighter").name("战斗机").categoryId("air-unit").sidc("A*A*F*FA---").affiliation("friend").symbolType("unit").hierarchy(" fighter").build());
        addSymbol(MilitarySymbol.builder().id("air-bomber").name("轰炸机").categoryId("air-unit").sidc("A*A*B*BA---").affiliation("friend").symbolType("unit").hierarchy(" bomber").build());
        addSymbol(MilitarySymbol.builder().id("air-transport").name("运输机").categoryId("air-unit").sidc("A*A*T*TA---").affiliation("friend").symbolType("unit").hierarchy(" transport").build());
        addSymbol(MilitarySymbol.builder().id("air-reconnaissance").name("侦察机").categoryId("air-unit").sidc("A*A*R*RC---").affiliation("friend").symbolType("unit").hierarchy(" reconnaissance").build());
        addSymbol(MilitarySymbol.builder().id("air-attack").name("攻击机").categoryId("air-unit").sidc("A*A*A*AT---").affiliation("friend").symbolType("unit").hierarchy(" attack").build());
        addSymbol(MilitarySymbol.builder().id("air-c2").name("预警机").categoryId("air-unit").sidc("A*A*Q*AE---").affiliation("friend").symbolType("unit").hierarchy(" C2").build());
        
        // 导弹/太空
        createCategory(SymbolCategory.builder().id("missile-unit").name("导弹/太空单位").code("M").description("Missile/Space Symbols").build());
        addSymbol(MilitarySymbol.builder().id("missile-ground").name("地对地导弹").categoryId("missile-unit").sidc("M*M*G*MS---").affiliation("friend").symbolType("unit").hierarchy(" ground missile").build());
        addSymbol(MilitarySymbol.builder().id("satellite").name("卫星").categoryId("missile-unit").sidc("M*M*S*SS---").affiliation("friend").symbolType("unit").hierarchy(" satellite").build());
        
        // 情报/电子战
        createCategory(SymbolCategory.builder().id("intel-unit").name("情报/电子战单位").code("I").description("Intelligence/EW Symbols").build());
        addSymbol(MilitarySymbol.builder().id("sigint").name("信号情报").categoryId("intel-unit").sidc("I*I*S*SI---").affiliation("friend").symbolType("unit").hierarchy(" SIGINT").build());
        addSymbol(MilitarySymbol.builder().id("ew-jamming").name("电子干扰").categoryId("intel-unit").sidc("I*I*E*EJ---").affiliation("friend").symbolType("unit").hierarchy(" EW jamming").build());
        addSymbol(MilitarySymbol.builder().id("cyber").name("网络战").categoryId("intel-unit").sidc("I*I*C*CY---").affiliation("friend").symbolType("unit").hierarchy(" cyber").build());
        
        // 民事
        createCategory(SymbolCategory.builder().id("civilian-unit").name("民事/控制").code("C").description("Civilian/Control Symbols").build());
        addSymbol(MilitarySymbol.builder().id("police").name("公安/警察").categoryId("civilian-unit").sidc("C*P*P*GP---").affiliation("neutral").symbolType("unit").hierarchy(" police").build());
        addSymbol(MilitarySymbol.builder().id("firefighter").name("消防").categoryId("civilian-unit").sidc("C*F*F*FF---").affiliation("neutral").symbolType("unit").hierarchy(" fire").build());
        addSymbol(MilitarySymbol.builder().id("medical").name("医疗").categoryId("civilian-unit").sidc("C*M*M*MH---").affiliation("neutral").symbolType("unit").hierarchy(" medical").build());
        
        // 敌方
        addSymbol(MilitarySymbol.builder().id("enemy-infantry").name("敌方步兵").categoryId("ground-unit").sidc("S*G*G*GPS--").affiliation("hostile").symbolType("unit").hierarchy(" infantry").build());
        addSymbol(MilitarySymbol.builder().id("enemy-armor").name("敌方装甲").categoryId("ground-unit").sidc("S*G*A*GPS--").affiliation("hostile").symbolType("unit").hierarchy(" armor").build());
        addSymbol(MilitarySymbol.builder().id("enemy-air").name("敌方飞机").categoryId("air-unit").sidc("A*A*F*FA---").affiliation("hostile").symbolType("unit").hierarchy(" fighter").build());
        
        // 战术符号
        addSymbol(MilitarySymbol.builder().id("hq").name("指挥部").categoryId("ground-unit").sidc("S*G*G*H----").affiliation("friend").symbolType("task").hierarchy(" headquarters").build());
        addSymbol(MilitarySymbol.builder().id("farp").name("前方弹药加油站").categoryId("ground-unit").sidc("S*G*G*F----").affiliation("friend").symbolType("task").hierarchy(" FARP").build());
        addSymbol(MilitarySymbol.builder().id("assembly-area").name("集结地域").categoryId("ground-unit").sidc("G*G*O*A----").affiliation("friend").symbolType("operation").hierarchy(" assembly area").build());
        addSymbol(MilitarySymbol.builder().id("attack-position").name("攻击阵地").categoryId("ground-unit").sidc("G*G*O*P----").affiliation("friend").symbolType("operation").hierarchy(" attack position").build());
        addSymbol(MilitarySymbol.builder().id("objective").name("目标地域").categoryId("ground-unit").sidc("G*G*O*O----").affiliation("friend").symbolType("operation").hierarchy(" objective").build());
        addSymbol(MilitarySymbol.builder().id("obstacle").name("障碍物").categoryId("ground-unit").sidc("O*O*O*O----").affiliation("unknown").symbolType("equipment").hierarchy(" obstacle").build());
        addSymbol(MilitarySymbol.builder().id("minefield").name("雷场").categoryId("ground-unit").sidc("O*O*M*M----").affiliation("unknown").symbolType("equipment").hierarchy(" minefield").build());
        addSymbol(MilitarySymbol.builder().id("checkpoint").name("检查站").categoryId("ground-unit").sidc("C*C*P*C----").affiliation("friend").symbolType("installations").hierarchy(" checkpoint").build());
        addSymbol(MilitarySymbol.builder().id("landing-zone").name("着陆区").categoryId("air-unit").sidc("L*L*L*L----").affiliation("friend").symbolType("operation").hierarchy(" landing zone").build());
        addSymbol(MilitarySymbol.builder().id("drop-zone").name("空投区").categoryId("air-unit").sidc("L*L*D*D----").affiliation("friend").symbolType("operation").hierarchy(" drop zone").build());
        addSymbol(MilitarySymbol.builder().id("ambulance").name("救护车").categoryId("civilian-unit").sidc("C*M*M*A----").affiliation("neutral").symbolType("equipment").hierarchy(" ambulance").build());
    }
    
    public MilitarySymbol addSymbol(MilitarySymbol symbol) {
        symbol.setId(symbol.getId() != null ? symbol.getId() : UUID.randomUUID().toString());
        symbol.setCreatedAt(System.currentTimeMillis());
        symbols.put(symbol.getId(), symbol);
        return symbol;
    }
    
    public MilitarySymbol getSymbol(String symbolId) { return symbols.get(symbolId); }
    public void deleteSymbol(String symbolId) { symbols.remove(symbolId); }
    
    public List<MilitarySymbol> searchSymbols(String keyword, String categoryId, String affiliation) {
        return symbols.values().stream()
                .filter(s -> {
                    if (keyword != null && !keyword.isEmpty()) {
                        String k = keyword.toLowerCase();
                        if (!s.getName().toLowerCase().contains(k) && !s.getSidc().toLowerCase().contains(k)) return false;
                    }
                    if (categoryId != null && !categoryId.equals(s.getCategoryId())) return false;
                    if (affiliation != null && !affiliation.equals(s.getAffiliation())) return false;
                    return true;
                }).toList();
    }
    
    public List<MilitarySymbol> getSymbolsByCategory(String categoryId) {
        return symbols.values().stream().filter(s -> categoryId.equals(s.getCategoryId())).toList();
    }
    
    public List<MilitarySymbol> getAllSymbols() { return new ArrayList<>(symbols.values()); }
    public SymbolCategory createCategory(SymbolCategory category) {
        category.setId(category.getId() != null ? category.getId() : UUID.randomUUID().toString());
        categories.put(category.getId(), category);
        return category;
    }
    public List<SymbolCategory> getAllCategories() { return new ArrayList<>(categories.values()); }
    
    public String getSymbolSVG(String symbolId) {
        MilitarySymbol symbol = symbols.get(symbolId);
        if (symbol == null) return "";
        String color = getAffiliationColor(symbol.getAffiliation());
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\"><circle cx=\"50\" cy=\"50\" r=\"40\" stroke=\"" + color + "\" stroke-width=\"4\" fill=\"none\"/></svg>";
    }
    
    public PlacedSymbol placeSymbol(String symbolId, double lon, double lat, double size, double rotation) {
        MilitarySymbol symbol = symbols.get(symbolId);
        if (symbol == null) throw new SymbolNotFoundException("符号不存在: " + symbolId);
        PlacedSymbol placed = new PlacedSymbol();
        placed.setId(UUID.randomUUID().toString());
        placed.setSymbolId(symbolId);
        placed.setSymbolName(symbol.getName());
        placed.setLon(lon);
        placed.setLat(lat);
        placed.setSize(size);
        placed.setRotation(rotation);
        placed.setAffiliation(symbol.getAffiliation());
        placed.setSidc(symbol.getSidc());
        placed.setPlacedAt(System.currentTimeMillis());
        return placed;
    }
    
    public SIDCInfo parseSIDC(String sidc) {
        if (sidc == null || sidc.length() != 15) throw new InvalidSIDCException("无效的SIDC: " + sidc);
        SIDCInfo info = new SIDCInfo();
        info.setSidc(sidc);
        info.setScheme(sidc.charAt(0));
        info.setCategory(sidc.substring(1, 2));
        info.setAffiliation(parseAffiliation(sidc.charAt(2)));
        info.setStatus(parseStatus(sidc.charAt(3)));
        return info;
    }
    
    private String parseAffiliation(char c) {
        return switch (c) { case 'F' -> "friend"; case 'H' -> "hostile"; case 'N' -> "neutral"; case 'U' -> "unknown"; default -> "unknown";
        };
    }
    private String parseStatus(char c) {
        return switch (c) { case 'A' -> "anticipated"; case 'P' -> "present"; default -> "unknown";
        };
    }
    
    private String getAffiliationColor(String affiliation) {
        return switch (affiliation) { case "friend" -> "#00FF00"; case "hostile" -> "#FF0000"; case "neutral" -> "#00FFFF"; case "unknown" -> "#FFFF00"; default -> "#808080";
        };
    }
    
    @lombok.Data @lombok.Builder
    public static class MilitarySymbol { private String id; private String name; private String categoryId; private String sidc; private String affiliation; private String symbolType; private String hierarchy; private Long createdAt; }
    @lombok.Data @lombok.Builder
    public static class SymbolCategory { private String id; private String name; private String code; private String description; }
    @lombok.Data @lombok.Builder
    public static class PlacedSymbol { private String id; private String symbolId; private String symbolName; private double lon; private double lat; private double size; private double rotation; private String affiliation; private String sidc; private Long placedAt; }
    @lombok.Data
    public static class SIDCInfo { private String sidc; private char scheme; private String category; private String affiliation; private String status; }
    public static class SymbolNotFoundException extends RuntimeException { public SymbolNotFoundException(String msg) { super(msg); } }
    public static class InvalidSIDCException extends RuntimeException { public InvalidSIDCException(String msg) { super(msg); } }
}
