use jni::{EnvUnowned, objects::JClass, sys::jstring};
use jni_fn::jni_fn;
use vulkano::instance::{Instance, InstanceCreateInfo};
use vulkano::VulkanLibrary;

#[jni_fn("top.kmiit.debughelper.utils.GpuUtils")]
pub fn getVulkanVersionNative<'local>(
    mut env: EnvUnowned<'local>,
    _class: JClass<'local>,
) -> jstring {
    let version_str = get_physical_device_version().unwrap_or_else(|_| "unknown".to_string());

    env.with_env(|env| env.new_string(version_str).map(|s| s.into_raw()))
        .resolve::<jni::errors::ThrowRuntimeExAndDefault>()
}

fn get_physical_device_version() -> Result<String, Box<dyn std::error::Error>> {
    let library = VulkanLibrary::new()?;
    let instance = Instance::new(library, InstanceCreateInfo::default())?;

    // Get the first physical device
    let physical = instance
        .enumerate_physical_devices()?
        .next()
        .ok_or("No physical device found")?;

    let version = physical.properties().api_version;

    // Return formatted version: Major.Minor.Patch
    Ok(format!("{}.{}.{}", version.major, version.minor, version.patch))
}
