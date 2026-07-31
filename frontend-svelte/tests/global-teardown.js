async function globalTeardown(config) {
  console.log('🧹 Очистка после тестов...');
  
  // Останавливаем бэкенд процесс
  if (global.backendProcess) {
    console.log('🛑 Остановка бэкенда...');
    global.backendProcess.kill('SIGTERM');
    
    // Ждем завершения процесса
    await new Promise(resolve => {
      global.backendProcess.on('exit', resolve);
      setTimeout(resolve, 5000); // Таймаут 5 секунд
    });
    
    console.log('✅ Бэкенд остановлен');
  }
  
  console.log('✅ Очистка завершена');
}

export default globalTeardown;
