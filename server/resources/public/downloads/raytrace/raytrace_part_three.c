#include <stdio.h>
#include <stdlib.h>
#include <string.h>		// for memcpy
#include <stdint.h>		// for uint8 and uint32
#include <fcntl.h>		// for open()
#include <math.h>		// acosf, sqrtf
#include <float.h>		// for FLT_MAX
#include <termios.h>
#include <sys/mman.h>	// for mmap()
#include <sys/ioctl.h>	// for framebuffer access
#include <linux/fb.h>	// for framebuffer access

// OS RELATED FUNCTIONS

uint8_t *framebuff_p;
struct fb_fix_screeninfo finfo;
struct fb_var_screeninfo vinfo;

void framebuffer_init( )
{

	int fb_fd = open( "/dev/fb0" , O_RDWR );

	// get and set screen information
	
	ioctl( fb_fd, FBIOGET_VSCREENINFO, &vinfo);
	
	vinfo.grayscale = 0;
	vinfo.bits_per_pixel = 32;

	ioctl( fb_fd, FBIOPUT_VSCREENINFO, &vinfo );
	ioctl( fb_fd, FBIOGET_VSCREENINFO, &vinfo );
	ioctl( fb_fd, FBIOGET_FSCREENINFO, &finfo );

	long screensize_l = vinfo.yres_virtual * finfo.line_length;

	framebuff_p = mmap( 0 , screensize_l , PROT_READ | PROT_WRITE , MAP_SHARED , fb_fd , ( off_t ) 0 );

}

/* creates pixel color suitable for the actual screen info */

uint32_t pixel_color( uint8_t r, uint8_t g, uint8_t b, struct fb_var_screeninfo *vinfo )
{
	return 	( r << vinfo->red.offset ) | 
			( g << vinfo->green.offset ) | 
			( b << vinfo->blue.offset );
}

/* draws square in framebuffer */

void framebuffer_drawsquare( int x , int y , int size , uint32_t color )
{

	for ( int ay = y ; ay < y + size ; ay++  )
	{
		for ( int ax = x ; ax < x + size ; ax++ )
		{

			long location = ( ax + vinfo.xoffset ) * ( vinfo.bits_per_pixel / 8 ) + 
							( ay + vinfo.yoffset ) * finfo.line_length;

			*( ( uint32_t* )( framebuff_p + location ) ) = pixel_color( 
					( color >>24 ) & 0xFF , 
					( color >> 16 ) & 0xFF , 
					( color >> 8 ) & 0xFF, &vinfo );

		}
	}

}

/* gets character from standard input without newline */

int getch( void ) 
{	

 	struct termios old_opts;
	struct termios new_opts;
    tcgetattr( fileno( stdin ) , &old_opts );
    memcpy( &new_opts, &old_opts, sizeof( new_opts ) );
	// set one keypress mode
    new_opts.c_lflag &= ~(ICANON | ECHO | ECHOE | ECHOK | ECHONL | ECHOPRT | ECHOKE | ICRNL);
    tcsetattr( fileno( stdin ) , TCSANOW , &new_opts );
    char result = getchar( );
    tcsetattr( fileno( stdin ) , TCSANOW , &old_opts );
    return( result );

}

// END OF OS RELATED FUNCTIONS

// MATH FUNCTIONS

typedef struct _v3_t
{
	float x, y, z;
} v3_t;

/* add two vectors */

v3_t v3_add( v3_t a , v3_t b )
{
	v3_t v;
	
	v.x = a.x + b.x;
	v.y = a.y + b.y;
	v.z = a.z + b.z;
	
	return v;
}

/* substracts b from a */

v3_t v3_sub( v3_t a , v3_t b )
{
	v3_t v;
	
	v.x = a.x - b.x;
	v.y = a.y - b.y;
	v.z = a.z - b.z;
	
	return v;
}

/* creates dot product of two vectors */

float v3_dot( v3_t a , v3_t b )
{
	return a.x * b.x + a.y * b.y + a.z * b.z;
}

/* creates cross product of two vectors */

v3_t v3_cross( v3_t left , v3_t right)
{
	v3_t v;
	
	v.x = left.y * right.z - left.z * right.y;
	v.y = left.z * right.x - left.x * right.z;
	v.z = left.x * right.y - left.y * right.x;

	return v;
}

/* scales vector */

v3_t v3_scale( v3_t a , float f )
{
	v3_t v;

	v.x = a.x * f;
	v.y = a.y * f;
	v.z = a.z * f;
	
	return v;
}

/* returns squared length to avoid square root operation */

float v3_length( v3_t a )
{
	return sqrtf( a.x * a.x + a.y * a.y + a.z * a.z );
}

/* return angle between a and b in radians */

float v3_angle( v3_t a , v3_t b )
{

	return acosf( v3_dot( a, b ) / ( v3_length( a ) * v3_length( b ) ) );

}

/* resizes vector to desired length */

v3_t v3_resize( v3_t a , float length )
{
	float ratio = length / v3_length( a );
	v3_t v = v3_scale( a , ratio );

	return v;
}

/* returns squared length to avoid square root operation */

float v3_length_squared( v3_t a )
{
	return a.x * a.x + a.y * a.y + a.z * a.z;
}

/* returns line plane intersection point */

v3_t line_plane_intersection( v3_t line_a_p , v3_t line_b_p , v3_t plane_p , v3_t normal_v )
{

	// line - plane intersection point calculation with dot products :
	// C = A + dot(AP,N) / dot( AB,N) * AB

	v3_t is = { FLT_MAX , FLT_MAX , FLT_MAX };

	v3_t AB = v3_sub( line_b_p , line_a_p );
	v3_t AP = v3_sub( plane_p , line_a_p );

	float dotABN = v3_dot( AB , normal_v );
	float dotAPN = v3_dot( AP , normal_v );

	if ( dotABN < FLT_MIN * 10.0 || dotABN > - FLT_MIN * 10.0 )
	{
		// if dot product is not close to zero there is an intersection 

		float scale_f = dotAPN / dotABN;

		if ( scale_f > 0.0 )	// we are interested in the "forward" part of the line only
		{
			is = v3_add( line_a_p , v3_scale( AB , scale_f ) );
		}

	}

	return is;

}

/* projects given point to line */

v3_t point_line_projection( v3_t line_a_p , v3_t line_b_p , v3_t point )
{

	// project intersection point to center line of rectangle
	// C = A + dot(AP,AB) / dot(AB,AB) * AB
	
	v3_t AB = v3_sub( line_b_p , line_a_p );
	v3_t AP = v3_sub( point , line_a_p );
	
	float dotAPAB = v3_dot( AP , AB );
	float dotABAB = v3_dot( AB , AB );
	float scale_f = dotAPAB / dotABAB;

	v3_t proj_p = v3_add( line_a_p , v3_scale( AB , scale_f ) );

	return proj_p;
	
}

/* mirrors given point on given line */

v3_t point_line_mirror( v3_t line_a_p , v3_t line_b_p , v3_t point )
{

	v3_t light_proj_p = point_line_projection( line_a_p , line_b_p , point );
	v3_t light_mirr_v = v3_sub( light_proj_p , point );

	light_mirr_v = v3_add( light_proj_p , light_mirr_v );

	return light_mirr_v;

}

/* calculates average of the two colors */

uint32_t color_average( uint32_t a , uint32_t b )
{

	uint32_t ar = ( a >> 24 ) & 0xFF;
	uint32_t ag = ( a >> 16 ) & 0xFF;
	uint32_t ab = ( a >> 8  ) & 0xFF;

	uint32_t br = ( b >> 24 ) & 0xFF;
	uint32_t bg = ( b >> 16 ) & 0xFF;
	uint32_t bb = ( b >> 8  ) & 0xFF;

	uint32_t fr = ar + ( br - ar ) / 2;
	uint32_t fg = ag + ( bg - ag ) / 2;
	uint32_t fb = ab + ( bb - ab ) / 2;

	fr &= 0xFF;
	fg &= 0xFF;
	fb &= 0xFF; 

	return ( fr << 24 ) | ( fg << 16 ) | ( fb << 8 ) | 0xFF; 

}

// END OF MATH FUNCTIONS

// rect definition list - center point, left side center point, top side center point

typedef struct
{

	v3_t base_p;
	v3_t side_p;
	v3_t norm_v;

	float wth;
	float hth;

	float transparency_c;

	uint32_t col_diff_u;
	uint32_t col_spec_u;

} rect_t;

int rect_cnt_i = 2;
rect_t rectangles[ 2 ];

typedef struct
{
	rect_t* rect;
	v3_t isect_p;

} nearest_res_t;

nearest_res_t get_nearest_rect( v3_t start_p , v3_t end_p , rect_t* exclude_r )
{

	nearest_res_t result = { 0 };
	float dist_f = FLT_MAX;
	
	if ( rect_cnt_i > 0 )
	{

		// iterate through all rectangles
			
		for ( int index_r = 0 ; index_r < rect_cnt_i ; index_r ++ )
		{

			rect_t* rect = &rectangles[ index_r ];

			if ( rect == exclude_r ) continue;

			// project ray from camera focus point through window grid point to square plane

			v3_t isect_p = line_plane_intersection( start_p , end_p , rect->base_p , rect->norm_v );

			if ( isect_p.x != FLT_MAX )
			{

				// let's find if intersection point is in the rectangle

				v3_t proj_p = point_line_projection( rect->side_p , rect->base_p , isect_p );

				// check x and y distance of intersection point from square center

				float dist_x = v3_length_squared( v3_sub( proj_p , rect->base_p ) );
				float dist_y = v3_length_squared( v3_sub( proj_p , isect_p ) );

				// compare squared distances with squared distances of rectangle

				if ( dist_x < ( rect->wth / 2.0 ) * ( rect->wth / 2.0 ) && 
					 dist_y < ( rect->hth / 2.0 ) * ( rect->hth / 2.0 ) )
				{
					// cross point is inside square, let's calculate it's color based on light reflection angle

					float distance = v3_length_squared( v3_sub( isect_p , start_p ) );

					if ( distance < dist_f )
					{

						result.rect = rect;
						result.isect_p = isect_p;

						dist_f = distance;

					}
					
				}

			}

		}

	}

	return result;
	
}

void geometry_init( )
{

	v3_t points[ ] =
	{ 
		{ 0.0  , -30.0 , -50.0  } , { -50.0 , -30.0 , -50.0  } , { 0.0  , 10.0 , -50.0  } ,
		{ 50.0 ,   0.0 , -100.0 } , { 0.0   ,   0.0 , -100.0 } , { 50.0 , 40.0 , -100.0 }
	};

	int count = sizeof( points ) / sizeof( v3_t );

	for ( int index = 0 ; 
			  index < count ; 
			  index += 3 )
	{

		rect_t* rectangle = &rectangles[ index / 3 ];
		rectangle->base_p = points[ index ];
		rectangle->side_p = points[ index + 1 ];

		v3_t ab_v = v3_sub( points[ index + 1 ] , points[ index ] );
		v3_t cb_v = v3_sub( points[ index + 2 ] , points[ index ] );

		rectangle->norm_v = v3_cross( cb_v , ab_v );
		rectangle->wth = v3_length( ab_v ) * 2;
		rectangle->hth = v3_length( cb_v ) * 2;

		rectangle->col_diff_u = 0x333333FF;
		rectangle->col_spec_u = 0xFFFFFFFF;

	}

}

int main( )
{

	framebuffer_init( );

	geometry_init( );

	// set up light

	v3_t light_p = { 0.0 , 30.0 , 0.0 };
	
	// set up camera

	v3_t camera_focus_p = { 40.0 , 20.0 , 100.0 };		// camera focus point
	v3_t camera_target_p = { 20.0 , 0.0 , 0.0 };

	// create corresponding grid in screen and in camera window 

    int grid_cols_i = 200;

	v3_t screen_d = { vinfo.xres , vinfo.yres };					// screen dimensions
	v3_t window_d = { 100.0 , 100.0 * screen_d.y / screen_d.x };	// camera window dimensions
	
	float screen_step_size_f = screen_d.x / grid_cols_i;	// screen block size
	float window_step_size_f = window_d.x / grid_cols_i;	// window block size

	int grid_rows_i = screen_d.y / screen_step_size_f;

	while ( 1 )
	{

		// camera window normal and xz plane normal

		v3_t window_normal_v = v3_sub( camera_focus_p , camera_target_p );
		v3_t xzplane_normal_v = { 0.0 , 1.0 , 0.0 };
		
		// create horizontal and vertical window axises
		
		v3_t window_haxis_v = v3_cross( window_normal_v , xzplane_normal_v );
		v3_t window_vaxis_v = v3_cross( window_normal_v , window_haxis_v );
		
		// resize horizontal and vertical screen vector to window step size
		
		v3_t window_stepx_v = v3_resize( window_haxis_v , window_step_size_f );
		v3_t window_stepy_v = v3_resize( window_vaxis_v , window_step_size_f );
		
		// create rays going through the camera window quad starting from the top left corner

		for ( int row_i = 0 ; row_i < grid_rows_i ; row_i++ )
		{

			for ( int col_i = 0 ; col_i < grid_cols_i ; col_i++ )
			{
			
				v3_t window_grid_v = camera_target_p;

				window_grid_v = v3_add( window_grid_v , v3_scale( window_stepx_v , grid_cols_i / 2 - col_i ) );
				window_grid_v = v3_add( window_grid_v , v3_scale( window_stepy_v , - grid_rows_i / 2 + row_i ) );
				
				// ray/pixel location on screen

				int screen_grid_x = screen_step_size_f * col_i;
				int screen_grid_y = screen_step_size_f * row_i;

				// check for intersection

				uint32_t color = 0x000000FF;	// background color

				nearest_res_t result = get_nearest_rect( camera_focus_p , window_grid_v , NULL );

				if ( result.rect != NULL )
				{

					// check for direct connection with light for diffuse color

					nearest_res_t blocker_r = get_nearest_rect( result.isect_p , light_p , result.rect );

					if ( blocker_r.rect == NULL ) 
					{
						// diffuse color

						color = result.rect->col_diff_u;

						// specular color

						// mirror light point on normal vector to get perfect reflection

						v3_t light_mirr_p = point_line_mirror( result.isect_p , v3_add( result.isect_p , result.rect->norm_v ) , light_p );

						v3_t tofocus = v3_sub( camera_focus_p , result.isect_p );
						v3_t tomirrored = v3_sub( light_mirr_p , result.isect_p );

						float angle = v3_angle( tomirrored , tofocus );

						// the smaller the angle the closer the mirrored ray and the real ray are - reflection 

						float colorsp_f = ( float ) 0xFF * ( ( M_PI - angle ) / M_PI );
						uint32_t colorsp_u = ( uint8_t ) colorsp_f;
						colorsp_u = colorsp_u << 24 | colorsp_u << 16 | colorsp_u << 8 | 0xFF;

						color = color_average( color , colorsp_u );

					}
					else 
					{
						// shadow

						color = 0x111111FF;
					}

					// if rect is transparent calculate refraction and check for further intersections

					// reflect ray on intersection point and check for further intersections
				
				}
			
				framebuffer_drawsquare( screen_grid_x , screen_grid_y , screen_step_size_f , color );
				
			}

		}

		// look for keypress

		int code = getch( );

		if ( code == 67 ) camera_focus_p.x += 10.0;
		if ( code == 68 ) camera_focus_p.x -= 10.0;

	}

	return 0;
}
