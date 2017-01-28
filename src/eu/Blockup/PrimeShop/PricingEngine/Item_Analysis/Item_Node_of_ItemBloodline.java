package eu.Blockup.PrimeShop.PricingEngine.Item_Analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import eu.Blockup.PrimeShop.PrimeShop;
import eu.Blockup.PrimeShop.Other.Cofiguration_Handler;
import eu.Blockup.PrimeShop.Other.Item_Comparer;
import eu.Blockup.PrimeShop.PricingEngine.DataHandling.EvaluatedRecipe;
import eu.Blockup.PrimeShop.PricingEngine.DataHandling.Shop_Item_Stack;
import eu.Blockup.PrimeShop.PricingEngine.Item_Analysis.ReturnObjects.ReturnPrice;

public class Item_Node_of_ItemBloodline {
    private List<Recepie_Node_of_ItemBloodline> listOfRecepieNodes;
    private Shop_Item_Stack sqlItemStack;
    private PrimeShop plugin;
    private static int[] ignoreList = {
            256,257,258,267,292,306,307,308,309,417, // iron items to ignore
            283,284,285,286,294,314,315,316,317,418, // gold items to ignore
            425 // banners
    };

    public PrimeShop getPlugin() {
        return plugin;
    }

    private Recepie_Node_of_ItemBloodline parentNode;

    boolean write_Objects_to_SQL() {

        boolean result = true;
        result = result && PrimeShop.databaseHandler.save_Item_to_Databse(this.sqlItemStack.getSql_Item()).succesful;
        for (Recepie_Node_of_ItemBloodline temp_node : this.listOfRecepieNodes) {
            result = result && temp_node.write_Objects_to_SQL();
        }
        return result;
    }

    public Item_Node_of_ItemBloodline clone(Recepie_Node_of_ItemBloodline parent) {
        Item_Node_of_ItemBloodline result;

        result = new Item_Node_of_ItemBloodline(parent,
                this.sqlItemStack.clone(), this.plugin);

        for (Recepie_Node_of_ItemBloodline recpie : this.listOfRecepieNodes) {
            result.listOfRecepieNodes.add(recpie.clone(result));
        }

        return result;
    }

    public Item_Node_of_ItemBloodline(Recepie_Node_of_ItemBloodline parentNode,
            Shop_Item_Stack sqlItemStack, PrimeShop plugin) {
        super();
        this.plugin = plugin;
        this.listOfRecepieNodes = new ArrayList<Recepie_Node_of_ItemBloodline>();
        this.sqlItemStack = sqlItemStack;

        this.parentNode = parentNode;
    }

    public void grow_this_tree() {
        // Erzeuge eine Liste mit Rezepten
        List<EvaluatedRecipe> temp_listOfRecepies = this.plugin
                .get_Recipes_of_Item(this.sqlItemStack.getSql_Item()
                        .getMcItemId_as_ItemStack(1));

        // Wenn diese Liste nicht leer ist, dann...

        if (!(temp_listOfRecepies.isEmpty())) {

            // F�r jedes Rezept in der Liste

            for (EvaluatedRecipe recepie : temp_listOfRecepies) {

                // erzeuge ein neues Obejkt RecepieNode

                // System.out.println(recepie.getResultedItem().toString());

                Recepie_Node_of_ItemBloodline temp_recepie_node = new Recepie_Node_of_ItemBloodline(this);

                // System.out.println(temp_recepie_node);

                // Pr�fe ob Rezept Evil ist

                if (this.Recepie_results_in_parent_Item(recepie)) {
                    // System.out.println("Recipe is evil");
                    temp_recepie_node.setEvilRecepie(true);
                    temp_recepie_node.parentNode.parentNode.setEvilRecepie(true);
                }

                // System.out.println("crafting" + recepie.getResultedItem().toString());

                // Don't craft air, probably does not do anything because of the other one further down
                if (recepie.getResultedItem().getTypeId() == 0) { // if air don't try and craft it
                    // System.out.println("Skipping crafting of air");
                    continue;
                }

                // blacklisted recopies that are a single item -> nuggets
                // System.out.println("ingredients" + recepie.getImputItemList().toString());
                if (recepie.getImputItemList().size() == 1 && ArrayUtils.contains(ignoreList, recepie.getImputItemList().get(0).getTypeId())) {
                    // System.out.println("this item is a recipe based on item id " + recepie.getImputItemList().get(0).getTypeId());
                    continue;
                }

                // �bergibt den Result ItemStack des Bukkit Rezepts, damit
                // sp�ter das Preis/Amount Verh�ltnis ermittelt werden kann.
                temp_recepie_node.parentRecepieItemAmount = recepie
                        .getResultedItem().getAmount();

                // f�r jedes Imput Item aus dem Rezept
                for (ItemStack itemstack : recepie.getImputItemList()) {

                    if (itemstack.getType().equals(Material.AIR)) {
                        // in 1.11 mojang replaced a ton of null with air, this ignores that air.
                        // System.out.println("Recipe wants air... **NO**");
                        continue;
                    }

                    // speichere Item in das temp Objekt (writes itemstack
                    // as sqlitem as sqlitemstack as treenode to list)
                    Item_Node_of_ItemBloodline TEST_NODE = new Item_Node_of_ItemBloodline(
                            temp_recepie_node,
                            new Shop_Item_Stack(PrimeShop
                                    .get_Shop_Item_of_Itemstack(itemstack),
                                    itemstack.getAmount(), false), this.plugin);

                    if (!(temp_recepie_node.getEvilRecepie())) {
                        TEST_NODE.grow_this_tree();
                    }
                    temp_recepie_node.listOfTreeNode.add(TEST_NODE);
                }
                // }
                this.listOfRecepieNodes.add(temp_recepie_node);
            }
        }
    }

    private boolean Recepie_results_in_parent_Item(EvaluatedRecipe recepie) {
        if (this.parentNode != null && this.parentNode.parentNode != null) {
            ItemStack oponentItem = this.parentNode.parentNode.sqlItemStack
                    .getSql_Item().getMcItemId_as_ItemStack(1);
            for (ItemStack itemstack : recepie.getImputItemList()) {
                if (Item_Comparer.do_Items_match(itemstack, oponentItem,
                        true, false, true, true, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean Recepie_results_in_parent_of_parent_item_Item(EvaluatedRecipe recepie) {
        if (this.parentNode == null || this.parentNode.parentNode == null) return false;
        if (this.parentNode.parentNode.parentNode == null || this.parentNode.parentNode.parentNode.parentNode == null) {
            return false;
        }
        ItemStack oponentItem = this.parentNode.parentNode.parentNode.parentNode.sqlItemStack
                .getSql_Item().getMcItemId_as_ItemStack(1);
        for (ItemStack itemstack : recepie.getImputItemList()) {
            if (Item_Comparer.do_Items_match(itemstack, oponentItem,
                    true, false, true, true, true)) {
                return true;
            }
        }
        return false;
    }

    public ReturnPrice buy_this_item(boolean buy, double amount,
            boolean save_after_Transaction) {
        ReturnPrice resultPrice = new ReturnPrice();
        Random randomGenerator = new Random();

        if (!(this.listOfRecepieNodes.isEmpty())) {

            int amount_of_evil_Recepies = 0;
            int amount_of_found_Recepies = 0;

            for (Recepie_Node_of_ItemBloodline recepie : this.listOfRecepieNodes) {
                if (recepie.getEvilRecepie()) {
                    amount_of_evil_Recepies = amount_of_evil_Recepies + 1;
                }
                amount_of_found_Recepies = amount_of_found_Recepies + 1;
            }

            Recepie_Node_of_ItemBloodline recepie;
            Recepie_Node_of_ItemBloodline chousenRecepie;
            int index;

            if (amount_of_found_Recepies > amount_of_evil_Recepies) {
                do {
                    index = randomGenerator.nextInt(amount_of_found_Recepies);
                    recepie = this.listOfRecepieNodes.get(index);

                    if (recepie != null) {
                        if (!(recepie.getEvilRecepie())) {
                            if (!(recepie.listOfTreeNode.isEmpty())) {
                                break;
                            } else {
                            }
                        } else {
                        }
                    } else {
                    }
                } while (true);

                chousenRecepie = recepie;

                if (!(chousenRecepie.listOfTreeNode.isEmpty())) {
                    for (Item_Node_of_ItemBloodline temp_NodeItem : chousenRecepie.listOfTreeNode) {

                        double childAmount = temp_NodeItem.sqlItemStack
                                .getAmount();
                        int parentAmount = chousenRecepie.parentRecepieItemAmount;
                        double percentalItemAmount = (amount * (childAmount / parentAmount));

                        // /////////////////////////////////// // TODO
                        // AUSLAGERN!
                        ReturnPrice temp_Price;
                        temp_Price = temp_NodeItem.buy_this_item(buy,
                                percentalItemAmount, save_after_Transaction);
                        temp_Price.price += Cofiguration_Handler.taxValueItemIncreasesWhenItWasCrafted
                                * amount; // TODO Check if ok?
                        if (temp_Price.succesful) {
                            resultPrice.price = resultPrice.price
                                    + temp_Price.price;
                        } else { // TODO was passiert, wenn der Kauf
                            // fehlschl�gt? Gibt es alternativen?
                            resultPrice = temp_Price;
                            resultPrice.price = 0;
                            return resultPrice;
                        }
                        temp_Price = null;
                        // /////////////////////////////////////
                    }
                } else {
                    resultPrice = this.buy_this_Node_Item(buy, amount,
                            save_after_Transaction);
                }

            } else {
                // Keine Rezepte mehr ..kaufe Item

                index = randomGenerator.nextInt(amount_of_evil_Recepies);
                Recepie_Node_of_ItemBloodline evil_recepie = this.listOfRecepieNodes
                        .get(index);

                chousenRecepie = evil_recepie;

                if (!(chousenRecepie.listOfTreeNode.isEmpty())) {
                    for (Item_Node_of_ItemBloodline temp_NodeItem : chousenRecepie.listOfTreeNode) {

                        double childAmount = temp_NodeItem.sqlItemStack
                                .getAmount();
                        int parentAmount = chousenRecepie.parentRecepieItemAmount;
                        double percentalItemAmount = (amount * (childAmount / parentAmount));

                        // /////////////////////////////////// // TODO
                        // AUSLAGERN!
                        ReturnPrice temp_Price;
                        temp_Price = temp_NodeItem.buy_this_item(buy,
                                percentalItemAmount, save_after_Transaction);
                        temp_Price.price += Cofiguration_Handler.taxValueItemIncreasesWhenItWasCrafted
                                * amount;
                        if (temp_Price.succesful) {
                            resultPrice.price = resultPrice.price
                                    + temp_Price.price;
                        } else { // TODO was passiert, wenn der Kauf
                            // fehlschl�gt? Gibt es alternativen?
                            resultPrice = temp_Price;
                            resultPrice.price = 0;
                            return resultPrice;
                        }
                        temp_Price = null;
                        // /////////////////////////////////////

                    }
                } else {
                    resultPrice = this.buy_this_Node_Item(buy, amount,
                            save_after_Transaction);
                }
            }
        } else {
            // Keine Rezepte mehr ..kaufe Item
            resultPrice = this.buy_this_Node_Item(buy, amount,
                    save_after_Transaction);
        }
        return resultPrice;
    }

    private ReturnPrice buy_this_Node_Item(boolean buy, double amount,
            boolean save_after_buy) {

        ReturnPrice result = new ReturnPrice();
        while (amount > 1) {
            amount = amount - 1;
            result = this.buy_in_small_steps(buy, 1, false, result);
        }
        if (amount > 0) {
            result = this.buy_in_small_steps(buy, amount, save_after_buy,
                    result);
        }
        return result;
    }

    private ReturnPrice buy_in_small_steps(boolean buy, double amount,
            boolean save_after_buy, ReturnPrice result) {
        ReturnPrice temp_result;
        temp_result = this.sqlItemStack.getSql_Item().buyItem(buy, amount,
                save_after_buy);
        result.price = result.price + temp_result.price;
        result.succesful = result.succesful && temp_result.succesful;
        result.errorMessage = temp_result.errorMessage;
        return result;
    }

    public List<ItemStack> collect_all_involved_items() {

        List<ItemStack> result = new ArrayList<ItemStack>();
        result.add(this.sqlItemStack.getSql_Item().getMcItemId_as_ItemStack(1));

        for (Recepie_Node_of_ItemBloodline recepienode : this.listOfRecepieNodes) {
            result.addAll(recepienode.collect_all_involved_items());
        }

        return result;

    }

}
