package name.modid.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class ExampleMixin {
	@Inject(at = @At("HEAD"), method = "changeGameMode")
	private void onChangeGameMode(GameMode gameMode, CallbackInfoReturnable<Boolean> info) {
		ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
	}
}
