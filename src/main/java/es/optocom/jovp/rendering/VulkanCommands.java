package es.optocom.jovp.rendering;

import es.optocom.jovp.Items;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK13.*;

/**
 * Command buffers for Vulkan rendering
 *
 * @since 0.0.1
 */
class VulkanCommands {

  final Items items;
  final long commandPool;
  List<VkCommandBuffer> commandBuffers;

  /**
   * Creates the command pool and buffers
   *
   * @param items Items to draw to the command buffers
   *
   * @since 0.0.1
   */
  VulkanCommands(Items items) {
    this.items = items;
    commandPool = VulkanSetup.createCommandPool();
    createCommandBuffers();
  }

  /** destroy command pool and buffers */
  void destroy() {
    vkFreeCommandBuffers(VulkanSetup.logicalDevice.device, commandPool, commandsPointerBuffer(commandBuffers));
    VulkanSetup.destroyCommandPool(commandPool);
  }

  /** do a render pass */
  void renderPass(int imageIndex) {
    try (MemoryStack stack = stackPush()) {
      VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
          .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
      VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
          .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
          .renderPass(VulkanSetup.swapChain.renderPass);
      VkRect2D renderArea = VkRect2D.calloc(stack).offset(VkOffset2D.calloc(stack)
          .set(0, 0)).extent(VulkanSetup.swapChain.extent);
      renderPassInfo.renderArea(renderArea);
      VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
      clearValues.get(0).color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
      clearValues.get(1).depthStencil().set(1.0f, 0);
      renderPassInfo.pClearValues(clearValues);
      VkCommandBuffer commandBuffer = commandBuffers.get(imageIndex);
      int result = vkBeginCommandBuffer(commandBuffer, beginInfo);
      if (result != VK_SUCCESS)
        throw new AssertionError("Failed to begin recording command buffers: " +
            VulkanSetup.translateVulkanResult(result));
      renderPassInfo.framebuffer(VulkanSetup.swapChain.frameBuffers.get(imageIndex));
      vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
      {
        // For monocular view there is only 1 view pass, for binocular 0 is left eye, 1
        // is right eye
        for (int eye = 0; eye < VulkanSetup.swapChain.viewPasses.size(); eye++) {
          ViewPass viewPass = VulkanSetup.swapChain.viewPasses.get(eye);
          vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, viewPass.graphicsPipeline);
          for (Item item : items)
            if (item.show) { // Check if item is to be shown
              item.buffers.updateUniforms(imageIndex, eye);
              ItemBuffers buffer = item.buffers;
              if (item.update) {
                buffer.update();
                item.update = false;
              }
              LongBuffer vertexBuffers = stack.longs(buffer.vertexBuffer);
              LongBuffer offsets = stack.longs(0);
              vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
              vkCmdBindIndexBuffer(commandBuffer, buffer.indexBuffer, 0, VK_INDEX_TYPE_UINT32);
              vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                  viewPass.pipelineLayout, 0,
                  stack.longs(buffer.descriptorSets.get(imageIndex)), null);
              vkCmdDrawIndexed(commandBuffer, item.model.length, 1,
                  0, 0, 0);
            }
        }
      }
      vkCmdEndRenderPass(commandBuffer);
      result = vkEndCommandBuffer(commandBuffer);
      if (result != VK_SUCCESS)
        throw new AssertionError("Failed to record command buffer: " +
            VulkanSetup.translateVulkanResult(result));
    }
  }

  /** create command buffers for each command pool */
  private void createCommandBuffers() {
    int size = VulkanSetup.swapChain.frameBuffers.size();
    commandBuffers = new ArrayList<>(size);
    try (MemoryStack stack = stackPush()) {
      VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
          .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO).commandPool(commandPool)
          .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY).commandBufferCount(size);
      PointerBuffer pCommandBuffers = stack.mallocPointer(size);
      int result = vkAllocateCommandBuffers(VulkanSetup.logicalDevice.device, allocInfo, pCommandBuffers);
      if (result != VK_SUCCESS)
        throw new AssertionError("Failed to allocate command buffers: " +
            VulkanSetup.translateVulkanResult(result));
      for (int i = 0; i < size; i++)
        commandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), VulkanSetup.logicalDevice.device));
    }
    for (int image = 0; image < size; image++)
      renderPass(image);
  }

  /** list to pointer buffer */
  private static PointerBuffer commandsPointerBuffer(List<? extends Pointer> list) {
    MemoryStack stack = stackGet();
    PointerBuffer buffer = stack.mallocPointer(list.size());
    list.forEach(buffer::put);
    return buffer.rewind();
  }

}