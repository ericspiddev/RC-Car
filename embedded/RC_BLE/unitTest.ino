
#include <AUnit.h>

test(Decoding)
{
  uint8_t command = 0xff;
   decodeCommand(command);
   assertEqual(steering, 3);
   assertEqual(setDirection, 3);
   assertEqual(setCarSpeed, 15);
  
}
