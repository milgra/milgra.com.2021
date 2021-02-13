#include "clcsrc.h"


int main( 	int			theCount ,
			const char* theTokens[ ] )
{

	struct FirstClass* first;
	
	first = FirstClass_alloc( );
	FirstClass_sayHello( first );
	free_object( first );
	
    return 0;
	
}
