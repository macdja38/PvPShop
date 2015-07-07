package eu.Blockup.PrimeShop.PricingEngine.Enchantments;

public class EnchantmentData {
    private int enchantmentID; // deprecated
    private String name; // added by tubelius 20140913
    private int enchantmentLevel;
    double price;

    public EnchantmentData(int enchantmentID, int enchantmentLevel,
            double enchantmentPrice, String name) {

        this.enchantmentID = enchantmentID;
        this.enchantmentLevel = enchantmentLevel;
        this.price = enchantmentPrice;
        this.name = name;
    }

    public int getEnchantmentID() {
        return enchantmentID;
    }

    public int getEnchantmentLevel() {
        return enchantmentLevel;
    }

    public double getPrice() {
        return price;
    }

    // @SuppressWarnings("deprecation")
    public String getName() {
        // //backward compatibility
        // if (name == null) {
        // name = Enchantment.getById(enchantmentID).getName();
        // }
        // return name
        return name;
    }
}
