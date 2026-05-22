const { chromium } = require('playwright');

async function globalSetup(config) {
  console.log('🚀 Запуск глобальной настройки тестов...');
  
  // Запускаем бэкенд сервер для тестов
  const backendProcess = require('child_process').spawn('mvn', [
    'spring-boot:run',
    '-Dspring-boot.run.profiles=test',
    '-Dspring-boot.run.jvmArguments=-Xmx512m'
  ], {
    cwd: '../backend',
    stdio: 'pipe'
  });
  
  // Ждем запуска бэкенда
  console.log('⏳ Ожидание запуска бэкенда...');
  await new Promise(resolve => setTimeout(resolve, 30000)); // 30 секунд
  
  // Проверяем что бэкенд запущен
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();
  
  try {
    const response = await page.goto('http://localhost:8080/api/companies/test');
    if (response.status() === 200) {
      console.log('✅ Бэкенд успешно запущен');
    } else {
      console.log('❌ Бэкенд не отвечает корректно');
    }
  } catch (error) {
    console.log('❌ Ошибка при проверке бэкенда:', error.message);
  }
  
  await browser.close();
  
  // Сохраняем процесс для последующего использования
  global.backendProcess = backendProcess;
  
  console.log('✅ Глобальная настройка завершена');
}

module.exports = globalSetup;
