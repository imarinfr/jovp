package es.optocom.jovp.rendering;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toSet;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.AMDDisplayNativeHdr.VK_COLOR_SPACE_DISPLAY_NATIVE_AMD;
import static org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.vkCreateDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_ADOBERGB_LINEAR_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_ADOBERGB_NONLINEAR_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_BT2020_LINEAR_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_BT709_LINEAR_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_BT709_NONLINEAR_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_DCI_P3_LINEAR_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_DCI_P3_NONLINEAR_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_DISPLAY_P3_LINEAR_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_DISPLAY_P3_NONLINEAR_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_DOLBYVISION_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_EXTENDED_SRGB_LINEAR_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_EXTENDED_SRGB_NONLINEAR_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_HDR10_HLG_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_HDR10_ST2084_EXT;
import static org.lwjgl.vulkan.EXTSwapchainColorspace.VK_COLOR_SPACE_PASS_THROUGH_EXT;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
import static org.lwjgl.vulkan.KHRMultiview.VK_KHR_MULTIVIEW_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSharedPresentableImage.VK_PRESENT_MODE_SHARED_CONTINUOUS_REFRESH_KHR;
import static org.lwjgl.vulkan.KHRSharedPresentableImage.VK_PRESENT_MODE_SHARED_DEMAND_REFRESH_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_NATIVE_WINDOW_IN_USE_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_SURFACE_LOST_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_RELAXED_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_TRANSFER_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_API_VERSION_MAJOR;
import static org.lwjgl.vulkan.VK10.VK_API_VERSION_MINOR;
import static org.lwjgl.vulkan.VK10.VK_API_VERSION_PATCH;
import static org.lwjgl.vulkan.VK10.VK_API_VERSION_VARIANT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_ERROR_DEVICE_LOST;
import static org.lwjgl.vulkan.VK10.VK_ERROR_EXTENSION_NOT_PRESENT;
import static org.lwjgl.vulkan.VK10.VK_ERROR_FEATURE_NOT_PRESENT;
import static org.lwjgl.vulkan.VK10.VK_ERROR_FORMAT_NOT_SUPPORTED;
import static org.lwjgl.vulkan.VK10.VK_ERROR_INCOMPATIBLE_DRIVER;
import static org.lwjgl.vulkan.VK10.VK_ERROR_INITIALIZATION_FAILED;
import static org.lwjgl.vulkan.VK10.VK_ERROR_LAYER_NOT_PRESENT;
import static org.lwjgl.vulkan.VK10.VK_ERROR_MEMORY_MAP_FAILED;
import static org.lwjgl.vulkan.VK10.VK_ERROR_OUT_OF_DEVICE_MEMORY;
import static org.lwjgl.vulkan.VK10.VK_ERROR_OUT_OF_HOST_MEMORY;
import static org.lwjgl.vulkan.VK10.VK_ERROR_TOO_MANY_OBJECTS;
import static org.lwjgl.vulkan.VK10.VK_EVENT_RESET;
import static org.lwjgl.vulkan.VK10.VK_EVENT_SET;
import static org.lwjgl.vulkan.VK10.VK_FALSE;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D24_UNORM_S8_UINT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT_S8_UINT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_DEPTH_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_STENCIL_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_TILING_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_VIEW_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_INCOMPLETE;
import static org.lwjgl.vulkan.VK10.VK_NOT_READY;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_CPU;
import static org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU;
import static org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU;
import static org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.VK_TIMEOUT;
import static org.lwjgl.vulkan.VK10.VK_TRUE;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkAllocateMemory;
import static org.lwjgl.vulkan.VK10.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkBindImageMemory;
import static org.lwjgl.vulkan.VK10.vkCmdPipelineBarrier;
import static org.lwjgl.vulkan.VK10.vkCreateCommandPool;
import static org.lwjgl.vulkan.VK10.vkCreateImage;
import static org.lwjgl.vulkan.VK10.vkCreateImageView;
import static org.lwjgl.vulkan.VK10.vkDestroyCommandPool;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkEnumerateDeviceExtensionProperties;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceLayerProperties;
import static org.lwjgl.vulkan.VK10.vkFreeCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkGetImageMemoryRequirements;
import static org.lwjgl.vulkan.VK10.vkGetInstanceProcAddr;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceFeatures;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceFormatProperties;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceMemoryProperties;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;
import static org.lwjgl.vulkan.VK10.vkQueueWaitIdle;
import static org.lwjgl.vulkan.VK11.VK_ERROR_OUT_OF_POOL_MEMORY;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkFormatProperties;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceLimits;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

/**
 *
 * A record with all the settings used for Vulkan
 *
 * @since 0.0.1
 */
class VulkanSetup {

    /** General settings */
    static final String TITLE = "JOVP Vulkan Engine";
    static final String ENGINE = "No engine";
    static final int MAJOR = 1; // Vulkan version
    static final int MINOR = 3;
    static final int PATCH = 0;
    static final int VK_API_VERSION = VK_API_VERSION_1_3;
    static final int UINT32_MAX = 0xFFFFFFFF;
    static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
    static final int MODEL_SIZEOF = (3 + 2) * Float.BYTES;
    // Vulkan instances
    static VkInstance instance;
    static boolean validationLayers;
    static long messenger;
    static boolean apiDump;
    static long surface;
    static List<VkPhysicalDevice> physicalDevices;
    static VkPhysicalDevice physicalDevice;
    static LogicalDevice logicalDevice;
    static SwapChain swapChain;
    static long commandPool = 0;
    static Observer observer;

    /** clean after use */
    static void cleanup() {
        instance = null;
        validationLayers = true;
        messenger = -1;
        apiDump = true;
        surface = -1;
        physicalDevices = null;
        physicalDevice = null;
        logicalDevice = null;
        swapChain = null;
        observer = null;
    }

    /** result translator */
    static String translateVulkanResult(int result) {
        return switch (result) {
            // success codes
            case VK_SUCCESS -> "Command successfully completed.";
            case VK_NOT_READY -> "A fence or query has not yet completed.";
            case VK_TIMEOUT -> "A wait operation has not completed in the specified time.";
            case VK_EVENT_SET -> "An event is signaled.";
            case VK_EVENT_RESET -> "An event is unsignaled.";
            case VK_INCOMPLETE -> "A return array was too small for the result.";
            case VK_SUBOPTIMAL_KHR -> "A swapchain no longer matches the surface properties exactly, " +
                    "but can still be used to present to the surface successfully.";
            // error codes
            case VK_ERROR_OUT_OF_HOST_MEMORY -> "A host memory allocation has failed.";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY -> "A device memory allocation has failed.";
            case VK_ERROR_INITIALIZATION_FAILED -> "Initialization of an object could not be completed " +
                    "for implementation-specific reasons.";
            case VK_ERROR_DEVICE_LOST -> "The logical or physical device has been lost.";
            case VK_ERROR_MEMORY_MAP_FAILED -> "Mapping of a memory object has failed.";
            case VK_ERROR_LAYER_NOT_PRESENT -> "A requested layer is not present or could not be loaded.";
            case VK_ERROR_EXTENSION_NOT_PRESENT -> "A requested extension is not supported.";
            case VK_ERROR_FEATURE_NOT_PRESENT -> "A requested feature is not supported.";
            case VK_ERROR_INCOMPATIBLE_DRIVER -> "The requested version of Vulkan is not supported " +
                    "by the driver or is otherwise incompatible for implementation-specific reasons.";
            case VK_ERROR_TOO_MANY_OBJECTS -> "Too many objects of the type have already been created.";
            case VK_ERROR_FORMAT_NOT_SUPPORTED -> "A requested format is not supported on this device.";
            case VK_ERROR_SURFACE_LOST_KHR -> "A surface is no longer available.";
            case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR -> "The requested window is already connected to a " +
                    "VkSurfaceKHR, or to some other non-Vulkan API.";
            case VK_ERROR_OUT_OF_DATE_KHR -> "A surface has changed in such a way that it is " +
                    "no longer compatible with the swapchain, and further presentation requests using  " +
                    "the swapchain will fail. Applications must query the new surface properties and " +
                    "recreate their swapchain if they wish to continue presenting to the surface.";
            case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR -> "The display used by a swapchain does not use " +
                    "the same presentable image layout, or is incompatible in a way that prevents sharing an image.";
            case VK_ERROR_VALIDATION_FAILED_EXT -> "A validation layer found an error.";
            case VK_ERROR_OUT_OF_POOL_MEMORY -> "Out of pool memory.";
            default -> String.format("%s [%d]", "Unknown", result);
        };
    }

    /** Get physical device properties */
    static String getPhysicalDeviceProperties(VkPhysicalDevice physicalDevice) {
        StringBuilder physicalDeviceInfo = new StringBuilder("Properties of ");
        physicalDeviceInfo.append(physicalDeviceName(physicalDevice)).append(":\n");
        VkPhysicalDeviceProperties properties = getDeviceProperties(physicalDevice);
        VkPhysicalDeviceLimits limits = properties.limits();
        String deviceType = switch (properties.deviceType()) {
            case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU -> "Integrated GPU";
            case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU -> "Discrete GPU";
            case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU -> "Virtual GPU";
            case VK_PHYSICAL_DEVICE_TYPE_CPU -> "CPU";
            default -> "UNKNOWN";
        };
        physicalDeviceInfo.append("\tInput Type: ").append(deviceType).append("\n");
        int apiVersionInteger = properties.apiVersion();
        String apiVersion = VK_API_VERSION_VARIANT(apiVersionInteger) + "." +
                VK_API_VERSION_MAJOR(apiVersionInteger) + "." +
                VK_API_VERSION_MINOR(apiVersionInteger) + "." +
                VK_API_VERSION_PATCH(apiVersionInteger);
        physicalDeviceInfo.append("\tAPI version: ").append(apiVersion).append("\n")
                .append("\tVendor ID: 0x").append(properties.vendorID()).append("\n")
                .append("\tInput ID: ").append(properties.deviceID()).append("\n")
                .append("\tLimits:\n")
                .append("\t\tMax dimension 1D: ").append(limits.maxImageDimension1D()).append(" pixels\n")
                .append("\t\tMax dimension 2D: ").append(limits.maxImageDimension2D()).append(" pixels\n")
                .append("\t\tMax dimension 3D: ").append(limits.maxImageDimension3D()).append(" pixels\n")
                .append("\t\tMemory: ").append(limits.maxComputeSharedMemorySize()).append(" bytes\n")
                .append("\t\tMax anisotropy: ").append(limits.maxSamplerAnisotropy()).append("\n")
                .append("\t\tNumber of viewports: ").append(limits.maxViewports()).append("\n");
        int[] dimensions = new int[] {
                limits.maxViewportDimensions().get(0),
                limits.maxViewportDimensions().get(1)
        };
        physicalDeviceInfo.append("\t\tViewport dimensions: ").append(Arrays.toString(dimensions)).append(" pixels\n");
        float[] bounds = new float[] {
                limits.viewportBoundsRange().get(0),
                limits.viewportBoundsRange().get(1)
        };
        physicalDeviceInfo.append("\t\tViewport range: ").append(Arrays.toString(bounds)).append(" pixels\n")
                .append("\t\tFramebuffer width: ").append(limits.maxFramebufferWidth()).append(" pixels\n")
                .append("\t\tFramebuffer height: ").append(limits.maxFramebufferHeight()).append(" pixels\n")
                .append("\t\tMax number of layers: ").append(limits.maxFramebufferLayers()).append("\n");
        float[] size = new float[] {
                limits.pointSizeRange().get(0),
                limits.pointSizeRange().get(1)
        };
        physicalDeviceInfo.append("\t\tPoint size range: ").append(Arrays.toString(size)).append(" pixels\n");
        float[] width = new float[] {
                limits.lineWidthRange().get(0),
                limits.lineWidthRange().get(1)
        };
        physicalDeviceInfo.append("\t\tLine width range: ").append(Arrays.toString(width)).append(" pixels\n");
        return physicalDeviceInfo.toString();
    }

    /** get swap chain support */
    static String getSwapChainSupport(VkPhysicalDevice physicalDevice) {
        StringBuilder swapChainDetails = new StringBuilder("Swap chain support for ");
        swapChainDetails.append(physicalDeviceName(physicalDevice)).append(":\n");
        try (MemoryStack stack = stackPush()) {
            SwapChainSupportDetails swapChainSupport = querySwapChainSupport(surface, physicalDevice, stack);
            VkSurfaceCapabilitiesKHR capabilities = swapChainSupport.capabilities;
            swapChainDetails.append("\tCapabilities:\n")
                    .append("\t\tMin Image count: ").append(capabilities.minImageCount()).append("\n")
                    .append("\t\tMax Image count: ").append(capabilities.maxImageCount()).append("\n")
                    .append("\t\tMax Image layers: ").append(capabilities.maxImageArrayLayers()).append("\n");
            int[] extent = new int[] {
                    capabilities.minImageExtent().width(),
                    capabilities.minImageExtent().height()
            };
            swapChainDetails.append("\t\tMin extent [width, height]: ").append(Arrays.toString(extent))
                    .append(" pixels\n");
            extent = new int[] {
                    capabilities.maxImageExtent().width(),
                    capabilities.maxImageExtent().height()
            };
            swapChainDetails.append("\t\tMax extent [width, height]: ").append(Arrays.toString(extent))
                    .append(" pixels\n");
            extent = new int[] {
                    capabilities.currentExtent().width(),
                    capabilities.currentExtent().height()
            };
            swapChainDetails.append("\t\tCurrent extent [width, height]: ").append(Arrays.toString(extent))
                    .append(" pixels\n");
            swapChainDetails.append("\tFormats: ");
            VkSurfaceFormatKHR.Buffer formats = swapChainSupport.formats;
            for (int i = 0; i < formats.capacity(); i++)
                swapChainDetails.append(formats.get(i).format()).append("; ");
            swapChainDetails.append("\n\t\tSee ")
                    .append("https://www.khronos.org/registry/vulkan/specs/1.3-extensions/man/html/VkFormat.html\n");
            String colorSpace = switch (formats.get(2).colorSpace()) {
                case VK_COLOR_SPACE_SRGB_NONLINEAR_KHR -> "SRGB nonlinear";
                case VK_COLOR_SPACE_DISPLAY_P3_NONLINEAR_EXT -> "P3 nonlinear";
                case VK_COLOR_SPACE_EXTENDED_SRGB_LINEAR_EXT -> "SRGB linear";
                case VK_COLOR_SPACE_DISPLAY_P3_LINEAR_EXT | VK_COLOR_SPACE_DCI_P3_LINEAR_EXT -> "P3 linear";
                case VK_COLOR_SPACE_DCI_P3_NONLINEAR_EXT -> "DCI P3 nonlinear";
                case VK_COLOR_SPACE_BT709_LINEAR_EXT -> "BT709 linear";
                case VK_COLOR_SPACE_BT709_NONLINEAR_EXT -> "BT707 nonlinear";
                case VK_COLOR_SPACE_BT2020_LINEAR_EXT -> "BT2020 linear";
                case VK_COLOR_SPACE_HDR10_ST2084_EXT -> "HDR10 ST2084";
                case VK_COLOR_SPACE_DOLBYVISION_EXT -> "Dolby Vision";
                case VK_COLOR_SPACE_HDR10_HLG_EXT -> "HDR10 HLG";
                case VK_COLOR_SPACE_ADOBERGB_LINEAR_EXT -> "Adobe RGB linear";
                case VK_COLOR_SPACE_ADOBERGB_NONLINEAR_EXT -> "Adobe RGB nonlinear";
                case VK_COLOR_SPACE_PASS_THROUGH_EXT -> "Pass through";
                case VK_COLOR_SPACE_EXTENDED_SRGB_NONLINEAR_EXT -> "Extended SRGB nonlinear";
                case VK_COLOR_SPACE_DISPLAY_NATIVE_AMD -> "Display native AMD";
                default -> "UNKNOWN";
            };
            swapChainDetails.append("\t\tColor Space: ").append(colorSpace).append("\n");
            IntBuffer presentModes = swapChainSupport.presentModes;
            swapChainDetails.append("\tPresent modes:\n");
            for (int i = 0; i < presentModes.capacity(); i++) {
                String presentMode = switch (presentModes.get(i)) {
                    case VK_PRESENT_MODE_IMMEDIATE_KHR -> "Immediate mode";
                    case VK_PRESENT_MODE_MAILBOX_KHR -> "Mailbox mode";
                    case VK_PRESENT_MODE_FIFO_KHR -> "FIFO mode";
                    case VK_PRESENT_MODE_FIFO_RELAXED_KHR -> "FIFO relaxed mode";
                    case VK_PRESENT_MODE_SHARED_DEMAND_REFRESH_KHR -> "Demand refresh mode";
                    case VK_PRESENT_MODE_SHARED_CONTINUOUS_REFRESH_KHR -> "Command refresh mode";
                    default -> "UNKNOWN";
                };
                swapChainDetails.append("\t\t").append(presentMode).append("\n");
            }
        }
        return swapChainDetails.toString();
    }

    /** physical device name */
    private static String physicalDeviceName(VkPhysicalDevice physicalDevice) {
        VkPhysicalDeviceProperties properties = getDeviceProperties(physicalDevice);
        String deviceName = String.valueOf(StandardCharsets.UTF_8.decode(properties.deviceName()));
        return deviceName.substring(0, deviceName.indexOf("\u0000"));
    }

    /** physical device properties */
    static VkPhysicalDeviceProperties getDeviceProperties(VkPhysicalDevice physicalDevice) {
        VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.create();
        vkGetPhysicalDeviceProperties(physicalDevice, properties);
        return properties;
    }

    /** collection to pointer buffer */
    static PointerBuffer asPointerBuffer(Collection<String> collection) {
        MemoryStack stack = stackGet();
        PointerBuffer buffer = stack.mallocPointer(collection.size());
        collection.stream()
                .map(stack::UTF8)
                .forEach(buffer::put);
        return buffer.rewind();
    }

    /** physical and logical device extensions parameters and utility functions */
    static final Set<String> ESSENTIAL_DEVICE_EXTENSIONS = Stream.of(
            new String[] {
                    VK_KHR_SWAPCHAIN_EXTENSION_NAME,
                    VK_KHR_MULTIVIEW_EXTENSION_NAME
            }).collect(toSet());
    static final Set<String> DESIRED_DEVICE_EXTENSIONS = Stream.of(
            new String[] {
                    VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME
            }).collect(toSet());

    /** validation layers, debugging setup and utility functions */
    static Set<String> VALIDATION_LAYERS;

    /** add validation layers */
    static void addValidationLayers() {
        if (validationLayers) {
            VALIDATION_LAYERS = new HashSet<>();
            VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_validation");
            VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_synchronization2");
            if (apiDump)
                VALIDATION_LAYERS.add("VK_LAYER_LUNARG_api_dump");
        } else
            VALIDATION_LAYERS = null;
    }

    /** check support for validation layers */
    static boolean checkValidationLayerSupport() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer layerCount = stack.ints(0);
            vkEnumerateInstanceLayerProperties(layerCount, null);
            VkLayerProperties.Buffer availableLayers = VkLayerProperties.malloc(layerCount.get(0), stack);
            vkEnumerateInstanceLayerProperties(layerCount, availableLayers);
            Set<String> availableLayerNames = availableLayers.stream()
                    .map(VkLayerProperties::layerNameString)
                    .collect(toSet());
            return availableLayerNames.containsAll(VALIDATION_LAYERS);
        }
    }

    /** setup debug messenger */
    static void setupDebugMessenger() {
        try (MemoryStack stack = stackPush()) {
            VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
            populateDebugMessengerCreateInfo(createInfo);
            LongBuffer pDebugMessenger = stack.longs(VK_NULL_HANDLE);
            int result = createDebugUtilsMessengerEXT(createInfo, pDebugMessenger);
            if (result != VK_SUCCESS)
                throw new RuntimeException("Failed to set up debug messenger: " + translateVulkanResult(result));
            messenger = pDebugMessenger.get(0);
        }
    }

    /** setup debug messenger */
    @SuppressWarnings("unused")
    static void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo) {
        debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
                .messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
                .pfnUserCallback(
                        (messageSeverity, messageType, pCallbackData, pUserData) -> debugCallback(pCallbackData));
    }

    /** destroy debug messenger */
    static void destroyDebugUtilsMessengerEXT() {
        if (vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT") != NULL) {
            vkDestroyDebugUtilsMessengerEXT(instance, messenger, null);
        }
    }

    /** debug callback */
    @SuppressWarnings("SameReturnValue")
    private static int debugCallback(long pCallbackData) {
        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
        System.err.println("Validation layer: " + callbackData.pMessageString());
        return VK_FALSE;
    }

    /** create debug messenger */
    private static int createDebugUtilsMessengerEXT(VkDebugUtilsMessengerCreateInfoEXT createInfo,
            LongBuffer pDebugMessenger) {
        if (vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != NULL) {
            return vkCreateDebugUtilsMessengerEXT(instance, createInfo, null, pDebugMessenger);
        }
        return VK_ERROR_EXTENSION_NOT_PRESENT;
    }

    /** instance parameters and utility functions */
    static PointerBuffer getRequiredExtensions() {
        PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();
        if (validationLayers) {
            MemoryStack stack = stackGet();
            if (glfwExtensions == null)
                throw new RuntimeException("Failed to get required extensions");
            PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + 1)
                    .put(glfwExtensions)
                    .put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
            return extensions.rewind();
        }
        return glfwExtensions;
    }

    /** check if device is suitable */
    static boolean isDeviceSuitable(long surface, VkPhysicalDevice physicalDevice) {
        QueueFamilyIndices indices = findQueueFamilies(surface, physicalDevice);
        boolean extensionsSupported = getDeviceExtensions(physicalDevice) != null;
        boolean swapChainAdequate = false;
        boolean anisotropySupported = false;
        if (extensionsSupported) {
            try (MemoryStack stack = stackPush()) {
                SwapChainSupportDetails swapChainSupport = querySwapChainSupport(surface, physicalDevice, stack);
                swapChainAdequate = swapChainSupport.formats.hasRemaining()
                        && swapChainSupport.presentModes.hasRemaining();
                VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.malloc(stack);
                vkGetPhysicalDeviceFeatures(physicalDevice, supportedFeatures);
                anisotropySupported = supportedFeatures.samplerAnisotropy();
            }
        }
        return indices.isComplete() && extensionsSupported && swapChainAdequate && anisotropySupported;
    }

    /** query family indices */
    static QueueFamilyIndices findQueueFamilies(long surface, VkPhysicalDevice physicalDevice) {
        VulkanSetup.QueueFamilyIndices indices = new QueueFamilyIndices();
        try (MemoryStack stack = stackPush()) {
            IntBuffer queueFamilyCount = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null);
            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0),
                    stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, queueFamilies);
            IntBuffer presentSupport = stack.ints(VK_FALSE);
            for (int i = 0; i < queueFamilies.capacity() || !indices.isComplete(); i++) {
                if ((queueFamilies.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0)
                    indices.graphicsFamily = i;
                vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, surface, presentSupport);
                if (presentSupport.get(0) == VK_TRUE)
                    indices.presentFamily = i;
            }
            return indices;
        }
    }

    /** list extension support for a physical device */
    static Set<String> listDeviceExtensionSupport(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer extensionCount = stack.ints(0);
            vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, extensionCount, null);
            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0),
                    stack);
            vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, extensionCount, availableExtensions);
            return availableExtensions.stream().map(VkExtensionProperties::extensionNameString).collect(toSet());
        }
    }

    /** check extension support for a physical device and set DEVICE_EXTENSIONS */
    public static Set<String> getDeviceExtensions(VkPhysicalDevice physicalDevice) {
        Set<String> ss = listDeviceExtensionSupport(physicalDevice);
        ArrayList<String> deviceExtensions = new ArrayList<>();
        for (String de : ESSENTIAL_DEVICE_EXTENSIONS) {
            if (!ss.contains(de)) { 
                System.out.println("Device does not support: " + de);
                return null;
            } else 
                deviceExtensions.add(de);
        }
        for (String dde : DESIRED_DEVICE_EXTENSIONS) {
            if (!ss.contains(dde)) 
                System.out.println("Device does not support desired extension (ignoring): " + dde);
            else 
                deviceExtensions.add(dde);
        }
        return deviceExtensions.stream().collect(Collectors.toSet());
    }

    /** find memory type */
    static int findMemoryType(int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.malloc();
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties);
        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties)
                return i;
        }
        memProperties.free();
        throw new RuntimeException("Failed to find suitable memory type");
    }

    /**
     * logical device, swap chain, render pass, and pipeline parameters and utility
     * functions
     */
    static QueueFamilyIndices queueFamilies() {
        return findQueueFamilies(surface, physicalDevice);
    }

    /** query support details for swap chain */
    static SwapChainSupportDetails querySwapChainSupport(long surface, VkPhysicalDevice physicalDevice,
            MemoryStack stack) {
        SwapChainSupportDetails details = new SwapChainSupportDetails();
        details.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, details.capabilities);
        IntBuffer count = stack.ints(0);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, count, null);
        if (count.get(0) != 0) {
            details.formats = VkSurfaceFormatKHR.malloc(count.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, count, details.formats);
        }
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, count, null);
        if (count.get(0) != 0) {
            details.presentModes = stack.mallocInt(count.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, count, details.presentModes);
        }
        return details;
    }

    /** obtain support details for swap chain */
    static SwapChainSupportDetails swapChainSupport(MemoryStack stack) {
        return querySwapChainSupport(surface, physicalDevice, stack);
    }

    /** find depth format */
    static int findDepthFormat() {
        IntBuffer formatCandidates = stackGet().ints(VK_FORMAT_D32_SFLOAT,
                VK_FORMAT_D32_SFLOAT_S8_UINT,
                VK_FORMAT_D24_UNORM_S8_UINT);
        try (MemoryStack stack = stackPush()) {
            VkFormatProperties props = VkFormatProperties.calloc(stack);
            for (int i = 0; i < formatCandidates.capacity(); ++i) {
                int format = formatCandidates.get(i);
                vkGetPhysicalDeviceFormatProperties(physicalDevice, format, props);
                if ((props.optimalTilingFeatures()
                        & VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT) == VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT)
                    return format;
            }
        }
        throw new RuntimeException("Failed to find supported format");
    }

    /** Create image */
    static void createImage(int width, int height, int mipLevels, int numSamples, int format, int usage, LongBuffer pTextureImage, LongBuffer pTextureImageMemory) {
        try (MemoryStack stack = stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .format(format)
                    .mipLevels(mipLevels)
                    .arrayLayers(1)
                    .samples(numSamples)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .usage(usage)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.extent().width(width)
                    .height(height)
                    .depth(1);
            int result = vkCreateImage(logicalDevice.device, imageInfo, null, pTextureImage);
            if (result != VK_SUCCESS)
                throw new RuntimeException("Failed to create image: " + translateVulkanResult(result));
            VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(logicalDevice.device, pTextureImage.get(0), memRequirements);
            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memRequirements.size())
                    .memoryTypeIndex(findMemoryType(memRequirements.memoryTypeBits(), VK_IMAGE_TILING_OPTIMAL));
            result = vkAllocateMemory(logicalDevice.device, allocInfo, null, pTextureImageMemory);
            if (result != VK_SUCCESS)
                throw new RuntimeException("Failed to allocate image memory: " + translateVulkanResult(result));
            vkBindImageMemory(logicalDevice.device, pTextureImage.get(0), pTextureImageMemory.get(0), 0);
        }
    }

    /** Create image view */
    static long createImageView(VkDevice device, long image, int format, int aspectFlags, int mipLevels) {
        try (MemoryStack stack = stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(image)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(format);
            viewInfo.subresourceRange().aspectMask(aspectFlags)
                    .baseMipLevel(0)
                    .levelCount(mipLevels)
                    .baseArrayLayer(0)
                    .layerCount(1);
            LongBuffer pImageView = stack.mallocLong(1);
            int result = vkCreateImageView(device, viewInfo, null, pImageView);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create texture image view: " + translateVulkanResult(result));
            return pImageView.get(0);
        }
    }

    /** transition image layout */
    static void transitionImageLayout(long commandPool, long image, int format, int newLayout, int mipLevels) {
        try (MemoryStack stack = stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .newLayout(newLayout)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(image);
            barrier.subresourceRange().baseMipLevel(0)
                    .levelCount(mipLevels)
                    .baseArrayLayer(0)
                    .layerCount(1);
            if (newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
                barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);
                if (format == VK_FORMAT_D32_SFLOAT_S8_UINT || format == VK_FORMAT_D24_UNORM_S8_UINT)
                    barrier.subresourceRange().aspectMask(
                            barrier.subresourceRange().aspectMask() | VK_IMAGE_ASPECT_STENCIL_BIT);
            } else
                barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            int sourceStage;
            int destinationStage;
            switch (newLayout) {
                case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL -> {
                    barrier.srcAccessMask(0)
                            .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                    sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                    destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                }
                case VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL -> {
                    barrier.srcAccessMask(0)
                            .dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT
                                    | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
                    sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                    destinationStage = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
                }
                case VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL -> {
                    barrier.srcAccessMask(0)
                            .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                    sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                    destinationStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
                }
                default -> throw new IllegalArgumentException("Unsupported layout transition");
            }
            VkCommandBuffer commandBuffer = beginCommand(commandPool);
            vkCmdPipelineBarrier(commandBuffer, sourceStage, destinationStage,
                    0, null, null, barrier);
            endCommand(commandPool, commandBuffer);
        }
    }

    // Model, transformations, and uniform buffers parameters utility functions
    static final Map<Class<?>, Integer> SIZEOF_CACHE = new HashMap<>();
    static {
        SIZEOF_CACHE.put(Byte.class, Byte.BYTES);
        SIZEOF_CACHE.put(Character.class, Character.BYTES);
        SIZEOF_CACHE.put(Short.class, Short.BYTES);
        SIZEOF_CACHE.put(Integer.class, Integer.BYTES);
        SIZEOF_CACHE.put(Float.class, Float.BYTES);
        SIZEOF_CACHE.put(Long.class, Long.BYTES);
        SIZEOF_CACHE.put(Double.class, Double.BYTES);
        SIZEOF_CACHE.put(Vector2f.class, 2 * Float.BYTES);
        SIZEOF_CACHE.put(Vector3f.class, 3 * Float.BYTES);
        SIZEOF_CACHE.put(Vector4f.class, 4 * Float.BYTES);
        SIZEOF_CACHE.put(Matrix4f.class, SIZEOF_CACHE.get(Vector4f.class));
    }

    /** create command pool */
    static long createCommandPool() {
        try (MemoryStack stack = stackPush()) {
            QueueFamilyIndices queueFamilyIndices = findQueueFamilies(surface, physicalDevice);
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .queueFamilyIndex(queueFamilyIndices.graphicsFamily)
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
            LongBuffer pCommandPool = stack.mallocLong(1);
            int result = vkCreateCommandPool(logicalDevice.device, poolInfo, null, pCommandPool);
            if (result != VK_SUCCESS)
                throw new AssertionError("Failed to create command pool: " + translateVulkanResult(result));
            return pCommandPool.get(0);
        }
    }

    /** destroy command pool */
    static void destroyCommandPool(long commandPool) {
        vkDestroyCommandPool(logicalDevice.device, commandPool, null);
    }

    /** begin GPU command */
    static VkCommandBuffer beginCommand(long commandPool) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandPool(commandPool)
                    .commandBufferCount(1);
            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            vkAllocateCommandBuffers(logicalDevice.device, allocInfo, pCommandBuffer);
            VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), logicalDevice.device);
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            vkBeginCommandBuffer(commandBuffer, beginInfo);
            return commandBuffer;
        }
    }

    /** end GPU command */
    static void endCommand(long commandPool, VkCommandBuffer commandBuffer) {
        try (MemoryStack stack = stackPush()) {
            vkEndCommandBuffer(commandBuffer);
            VkSubmitInfo.Buffer submitInfo = VkSubmitInfo.calloc(1, stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(commandBuffer));
            vkQueueSubmit(logicalDevice.graphicsQueue, submitInfo, VK_NULL_HANDLE);
            vkQueueWaitIdle(logicalDevice.graphicsQueue);
            vkFreeCommandBuffers(logicalDevice.device, commandPool, commandBuffer);
        }
    }

    /** handle queue family indices */
    static class QueueFamilyIndices {

        static final float PRIORITY = 1.0f;

        // We use Integer to use null as the empty value
        Integer graphicsFamily;
        Integer presentFamily;

        /** check both present and graphics families are present */
        boolean isComplete() {
            return graphicsFamily != null && presentFamily != null;
        }

        /** list unique graphics and present families */
        int[] unique() {
            return IntStream.of(graphicsFamily, presentFamily).distinct().toArray();
        }

    }

    /** handle swap chain support details */
    static class SwapChainSupportDetails {

        VkSurfaceCapabilitiesKHR capabilities;
        VkSurfaceFormatKHR.Buffer formats;
        IntBuffer presentModes;

    }

}
