package framework.telegram.ui.emoji.ios;

import androidx.annotation.NonNull;

import framework.telegram.ui.emoji.EmojiProvider;
import framework.telegram.ui.emoji.base.EmojiCategory;
import framework.telegram.ui.emoji.ios.category.ActivitiesCategory;
import framework.telegram.ui.emoji.ios.category.AnimalsAndNatureCategory;
import framework.telegram.ui.emoji.ios.category.FlagsCategory;
import framework.telegram.ui.emoji.ios.category.FoodAndDrinkCategory;
import framework.telegram.ui.emoji.ios.category.MyCategory;
import framework.telegram.ui.emoji.ios.category.ObjectsCategory;
import framework.telegram.ui.emoji.ios.category.SmileysAndPeopleCategory;
import framework.telegram.ui.emoji.ios.category.SymbolsCategory;
import framework.telegram.ui.emoji.ios.category.TravelAndPlacesCategory;

public final class IosEmojiProvider implements EmojiProvider {
    @Override
    @NonNull
    public EmojiCategory[] getCategories() {
        return new EmojiCategory[]{
                new MyCategory(),
                new SmileysAndPeopleCategory(),
                new AnimalsAndNatureCategory(),
                new FoodAndDrinkCategory(),
                new ActivitiesCategory(),
                new TravelAndPlacesCategory(),
                new ObjectsCategory(),
                new SymbolsCategory(),
                new FlagsCategory()
        };
    }
}
