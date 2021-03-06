package protocolsupport.protocol.packet.middle.clientbound.play;

import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.middle.ClientBoundMiddlePacket;
import protocolsupport.protocol.serializer.ItemStackSerializer;
import protocolsupport.protocol.serializer.StringSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.utils.types.NetworkItemStack;

public abstract class MiddleDeclareRecipes extends ClientBoundMiddlePacket {

	public MiddleDeclareRecipes(ConnectionImpl connection) {
		super(connection);
	}

	protected Recipe[] recipes;

	@Override
	public void readFromServerData(ByteBuf serverdata) {
		int count = VarNumberSerializer.readVarInt(serverdata);
		recipes = new Recipe[count];
		for (int i = 0; i < count; i++) {
			String id = StringSerializer.readVarIntUTF8String(serverdata);
			RecipeType type = RecipeType.getByInternalName(StringSerializer.readVarIntUTF8String(serverdata));
			recipes[i] = type.read(id, serverdata);
		}
	}

	public static class Ingredient {
		protected final NetworkItemStack[] possibleStacks;

		public Ingredient(ByteBuf serverdata) {
			int possibleStacksCount = VarNumberSerializer.readVarInt(serverdata);
			possibleStacks = new NetworkItemStack[possibleStacksCount];
			for (int i = 0; i < possibleStacksCount; i++) {
				possibleStacks[i] = ItemStackSerializer.readItemStack(serverdata);
			}
		}

		public NetworkItemStack[] getPossibleStacks() {
			return possibleStacks;
		}
	}

	public static class Recipe {
		protected final String id;
		protected final RecipeType type;

		public Recipe(String id, RecipeType type) {
			this.id = id;
			this.type = type;
		}

		public String getId() {
			return id;
		}

		public RecipeType getType() {
			return type;
		}
	}

	public static class ShapelessRecipe extends Recipe {
		protected final String group;
		protected final Ingredient[] ingredients;
		protected final NetworkItemStack result;

		public ShapelessRecipe(String id, ByteBuf data) {
			super(id, RecipeType.CRAFTING_SHAPELESS);

			group = StringSerializer.readVarIntUTF8String(data);
			int ingredientCount = VarNumberSerializer.readVarInt(data);
			ingredients = new Ingredient[ingredientCount];
			for (int j = 0; j < ingredientCount; j++) {
				ingredients[j] = new Ingredient(data);
			}
			result = ItemStackSerializer.readItemStack(data);
		}

		public String getGroup() {
			return group;
		}

		public Ingredient[] getIngredients() {
			return ingredients;
		}

		public NetworkItemStack getResult() {
			return result;
		}
	}

	public static class ShapedRecipe extends Recipe {
		protected final String group;
		protected final int width;
		protected final int height;
		protected final Ingredient[] ingredients;
		protected final NetworkItemStack result;

		public ShapedRecipe(String id, ByteBuf data) {
			super(id, RecipeType.CRAFTING_SHAPED);

			width = VarNumberSerializer.readVarInt(data);
			height = VarNumberSerializer.readVarInt(data);
			group = StringSerializer.readVarIntUTF8String(data);
			int ingredientCount = width * height;
			ingredients = new Ingredient[ingredientCount];
			for (int j = 0; j < ingredientCount; j++) {
				ingredients[j] = new Ingredient(data);
			}
			result = ItemStackSerializer.readItemStack(data);
		}

		public String getGroup() {
			return group;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public Ingredient[] getIngredients() {
			return ingredients;
		}

		public NetworkItemStack getResult() {
			return result;
		}
	}

	public static class SmeltingRecipe extends Recipe {
		protected final String group;
		protected final Ingredient ingredient;
		protected final NetworkItemStack result;
		protected final float exp;
		protected final int time;

		public SmeltingRecipe(String id, ByteBuf data) {
			super(id, RecipeType.SMELTING);

			group = StringSerializer.readVarIntUTF8String(data);
			ingredient = new Ingredient(data);
			result = ItemStackSerializer.readItemStack(data);
			exp = data.readFloat();
			time = VarNumberSerializer.readVarInt(data);
		}

		public String getGroup() {
			return group;
		}

		public Ingredient getIngredient() {
			return ingredient;
		}

		public NetworkItemStack getResult() {
			return result;
		}

		public float getExp() {
			return exp;
		}

		public int getTime() {
			return time;
		}
	}

	public enum RecipeType {
		CRAFTING_SHAPELESS {
			@Override
			public Recipe read(String id, ByteBuf data) {
				return new ShapelessRecipe(id, data);
			}
		},
		CRAFTING_SHAPED {
			@Override
			public Recipe read(String id, ByteBuf data) {
				return new ShapedRecipe(id, data);
			}
		},
		CRAFTING_SPECIAL_ARMORDYE,
		CRAFTING_SPECIAL_BOOKCLONING,
		CRAFTING_SPECIAL_MAPCLONING,
		CRAFTING_SPECIAL_MAPEXTENDING,
		CRAFTING_SPECIAL_FIREWORK_ROCKET,
		CRAFTING_SPECIAL_FIREWORK_STAR,
		CRAFTING_SPECIAL_FIREWORK_STAR_FADE,
		CRAFTING_SPECIAL_REPAIRITEM,
		CRAFTING_SPECIAL_TIPPEDARROW,
		CRAFTING_SPECIAL_BANNERDUPLICATE,
		CRAFTING_SPECIAL_BANNERADDPATTERN,
		CRAFTING_SPECIAL_SHIELDDECORATION,
		CRAFTING_SPECIAL_SHULKERBOXCOLORING,
		SMELTING {
			@Override
			public Recipe read(String id, ByteBuf data) {
				return new SmeltingRecipe(id, data);
			}
		};

		private static final Map<String, RecipeType> byInternalName = new HashMap<>();
		private final String internalName = name().toLowerCase();

		static {
			for (RecipeType recipeType : values()) {
				byInternalName.put(recipeType.getInternalName(), recipeType);
			}
		}

		public static RecipeType getByInternalName(String name) {
			RecipeType recipeType = byInternalName.get(name);
			if (recipeType == null) {
				throw new IllegalArgumentException(name + " is no valid recipe type");
			}
			return recipeType;
		}

		public String getInternalName() {
			return internalName;
		}

		public Recipe read(String id, ByteBuf serverdata) {
			return new Recipe(id, this);
		}

	}
}
