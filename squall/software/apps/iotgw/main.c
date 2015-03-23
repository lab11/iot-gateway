/* 
    Advertising using Gateway API
 */

#include <stdbool.h>
#include <stdint.h>
#include "nrf_gpio.h"
#include "ble_advdata.h"
#include "boards.h"
#include "nordic_common.h"
#include "softdevice_handler.h"
#include "ble_debug_assert_handler.h"
#include "led.h"


#define IS_SRVC_CHANGED_CHARACT_PRESENT 0                                 /**< Include or not the service_changed characteristic. if not enabled, the server's database cannot be changed for the lifetime of the device*/

#define USE_LEDS                        1
#define ADVERTISING_LED                 LED_0                             /**< Is on when device is advertising. */

#define APP_CFG_NON_CONN_ADV_TIMEOUT    0                                 /**< Time for which the device must be advertising in non-connectable mode (in seconds). 0 disables timeout. */
#define NON_CONNECTABLE_ADV_INTERVAL    MSEC_TO_UNITS(1000, UNIT_0_625_MS)   /**< The advertising interval for non-connectable advertisement (100 ms). This value can vary between 100ms to 10.24s). */

#define ADVERTISE_DEV_NAME              1                                 /**< Whether to advertise device name (may not fit if APP_DATA_LENGTH too big). */
#define DEV_NAME                        "Squall"

/** GATEWAY API PARAMETERS **/    

#define DESTINATION_SHORT_URL           "goo.gl/jRMxE0"                   /**< Shortened destination url w/o protocol (up to 14 characters) */
#define PROGRAM_TYPE                    0x7                               /**< Incentive program type level (0-F) */
#define RELIABILITY                     0x7                               /**< Reliability level (0-F) */

#define APP_DATA_LENGTH                 2                                 /**< Custom app data (up to 10 bytes) */
#define APP_DATA                        0x12,0x34

#define TIME_ACCESS                     1                                 /**< Gateway services requested. */
#define GPS_ACCESS                      1
#define ACCELEROMETER_ACCESS            0
#define AMBIENT_LIGHT_ACCESS            0
#define TEXT_INPUT_REQUEST              0
#define CAMERA_INPUT_REQUEST            0
#define WEB_UI_REQUEST                  0
#define IP_OVER_BLE_REQUEST             0


// information about the advertisement
ble_advdata_t                           advdata;
static ble_gap_adv_params_t             m_adv_params;                     /**< Parameters to be passed to the stack when starting advertising. */

static const uint8_t fixed_length_url[14] = DESTINATION_SHORT_URL;
static const uint8_t app_data[APP_DATA_LENGTH] = {APP_DATA};
static const uint8_t program_reliability = PROGRAM_TYPE<<4 | RELIABILITY;
static const uint8_t phone_services = TIME_ACCESS<<7 | GPS_ACCESS<<6 | ACCELEROMETER_ACCESS<<5 | AMBIENT_LIGHT_ACCESS<<4 |
                                      TEXT_INPUT_REQUEST<<3 | CAMERA_INPUT_REQUEST<<2 | WEB_UI_REQUEST<<1 | IP_OVER_BLE_REQUEST;


/**@brief Function for error handling, which is called when an error has occurred.
 *
 * @warning This handler is an example only and does not fit a final product. You need to analyze
 *          how your product is supposed to react in case of error.
 *
 * @param[in] error_code  Error code supplied to the handler.
 * @param[in] line_num    Line number where the handler is called.
 * @param[in] p_file_name Pointer to the file name.
 */
void app_error_handler(uint32_t error_code, uint32_t line_num, const uint8_t * p_file_name)
{
    // This call can be used for debug purposes during application development.
    // @note CAUTION: Activating this code will write the stack to flash on an error.
    //                This function should NOT be used in a final product.
    //                It is intended STRICTLY for development/debugging purposes.
    //                The flash write will happen EVEN if the radio is active, thus interrupting
    //                any communication.
    //                Use with care. Un-comment the line below to use.
    // ble_debug_assert_handler(error_code, line_num, p_file_name);

    // On assert, the system can only recover on reset.
    NVIC_SystemReset();
}


/**@brief Callback function for asserts in the SoftDevice.
 *
 * @details This function will be called in case of an assert in the SoftDevice.
 *
 * @warning This handler is an example only and does not fit a final product. You need to analyze
 *          how your product is supposed to react in case of Assert.
 * @warning On assert from the SoftDevice, the system can only recover on reset.
 *
 * @param[in]   line_num   Line number of the failing ASSERT call.
 * @param[in]   file_name  File name of the failing ASSERT call.
 */
void assert_nrf_callback(uint16_t line_num, const uint8_t * p_file_name)
{
    app_error_handler(0xDEADBEEF, line_num, p_file_name);
}

// Setup TX power and the device name
static void gap_params_init(void)
{
    uint32_t                err_code;
    ble_gap_conn_sec_mode_t sec_mode;

    // Set the power. Using really low (-30) doesn't really work
    sd_ble_gap_tx_power_set(4);

    // Make the connection open (no security)
    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&sec_mode);

    // Set the name of the device so its easier to find
    err_code = sd_ble_gap_device_name_set(&sec_mode,
                                          (const uint8_t *) DEV_NAME,
                                          strlen(DEV_NAME));
    APP_ERROR_CHECK(err_code);
}


/**@brief Function for initializing the Advertising functionality.
 *
 * @details Encodes the required advertising data and passes it to the stack.
 *          Also builds a structure to be passed to the stack when starting advertising.
 */
static void advertising_init(void)
{
    uint32_t                  err_code;

    // Use the simplest send adv packets only mode
    uint8_t                   flags = BLE_GAP_ADV_FLAG_BR_EDR_NOT_SUPPORTED;

    uint8_t i, gateway_api[16+APP_DATA_LENGTH];
    for (i=0; i<(16+APP_DATA_LENGTH); i++) {
        if (i<14)        gateway_api[i] = fixed_length_url[i];
        else if (i==14)  gateway_api[i] = program_reliability;
        else if (i==15)  gateway_api[i] = phone_services;
        else             gateway_api[i] = app_data[i-16];
    };

    ble_advdata_service_data_t  service_data;

    service_data.service_uuid = (((uint16_t)gateway_api[0])<<8) + gateway_api[1];
    service_data.data.p_data  = (uint8_t *) &gateway_api[2];
    service_data.data.size    = 14+APP_DATA_LENGTH;

    // Build and set advertising data.
    memset(&advdata, 0, sizeof(advdata));

    if (ADVERTISE_DEV_NAME) advdata.name_type = BLE_ADVDATA_FULL_NAME;
    advdata.flags.size              = sizeof(flags);
    advdata.flags.p_data            = &flags;
    // advdata.p_manuf_specific_data   = &manuf_specific_data;
    advdata.p_service_data_array    = &service_data;
    advdata.service_data_count      = 1;


    err_code = ble_advdata_set(&advdata, NULL);
    APP_ERROR_CHECK(err_code);

    // Initialize advertising parameters (used when starting advertising).
    memset(&m_adv_params, 0, sizeof(m_adv_params));

    m_adv_params.type        = BLE_GAP_ADV_TYPE_ADV_NONCONN_IND;
    m_adv_params.p_peer_addr = NULL;                             // Undirected advertisement.
    m_adv_params.fp          = BLE_GAP_ADV_FP_ANY;
    m_adv_params.interval    = NON_CONNECTABLE_ADV_INTERVAL;
    m_adv_params.timeout     = APP_CFG_NON_CONN_ADV_TIMEOUT;
}


/**@brief Function for starting advertising.
 */
static void advertising_start(void)
{
    uint32_t err_code;

    err_code = sd_ble_gap_adv_start(&m_adv_params);
    APP_ERROR_CHECK(err_code);

    if(USE_LEDS) led_toggle(ADVERTISING_LED);
}


/**@brief Function for initializing the BLE stack.
 *
 * @details Initializes the SoftDevice and the BLE event interrupt.
 */
static void ble_stack_init(void)
{

    // Initialize the SoftDevice handler module.
    // Use a really crappy clock because we want fast start
    SOFTDEVICE_HANDLER_INIT(NRF_CLOCK_LFCLKSRC_RC_250_PPM_8000MS_CALIBRATION, false);

    // Enable BLE stack
    uint32_t err_code;
    ble_enable_params_t ble_enable_params;
    memset(&ble_enable_params, 0, sizeof(ble_enable_params));
    ble_enable_params.gatts_enable_params.service_changed = IS_SRVC_CHANGED_CHARACT_PRESENT;
    err_code = sd_ble_enable(&ble_enable_params);
    APP_ERROR_CHECK(err_code);
}

/**@brief Function for doing power management.
 */
static void power_manage(void)
{
    uint32_t err_code = sd_app_evt_wait();
    APP_ERROR_CHECK(err_code);
}


/**
 * @brief Function for application main entry.
 */
int main(void)
{
    // Initialize.
    platform_init();

    ble_stack_init();

    gap_params_init();

    advertising_init();

    led_init(ADVERTISING_LED);
    led_off(ADVERTISING_LED);

    // Start execution.
    advertising_start();

    while (1) {
        power_manage();
    }
}

